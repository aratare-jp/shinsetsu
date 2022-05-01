(ns shinsetsu.db.bookmark
  (:require
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as helpers]
    [honey.sql :as sql]
    [taoensso.timbre :as log]
    [shinsetsu.db :refer [ds]]
    [shinsetsu.schema :as s]
    [malli.core :as m]
    [malli.error :as me]
    [clojure.string :as string])
  (:import [java.time Instant]
           [org.postgresql.util PSQLException]))

(defn create-bookmark
  [{:bookmark/keys [tab-id user-id] :as bookmark}]
  (if-let [err (m/explain s/bookmark-create-spec bookmark)]
    (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
    (try
      (log/info "Create new bookmark in tab" tab-id "for user" user-id)
      (jdbc/execute-one! ds (-> (helpers/insert-into :bookmark)
                                (helpers/values [bookmark])
                                (helpers/returning :*)
                                (sql/format)))
      (catch PSQLException e
        (log/error e)
        (case (.getSQLState e)
          "23503" (throw (ex-info "Nonexistent tab" {:error-type :invalid-input
                                                     :error-data {:bookmark/tab-id ["nonexistent"]}}))
          (throw (ex-info "Unknown error" {:error-type :unknown} e)))))))

(defn patch-bookmark
  [{:bookmark/keys [id user-id] :as bookmark}]
  (if-let [err (m/explain s/bookmark-patch-spec bookmark)]
    (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
    (let [bookmark (assoc bookmark :bookmark/updated (Instant/now))]
      (log/info "Update bookmark" id "for user" user-id)
      (try
        (jdbc/execute-one! ds (-> (helpers/update :bookmark)
                                  (helpers/set bookmark)
                                  (helpers/where [:= :bookmark/id id] [:= :bookmark/user-id user-id])
                                  (helpers/returning :*)
                                  (sql/format)))
        (catch PSQLException e
          (log/error e)
          (case (.getSQLState e)
            "23503" (throw (ex-info "Nonexistent tab" {:error-type :invalid-input
                                                       :error-data {:bookmark/tab-id ["nonexistent"]}}))
            (throw (ex-info "Unknown error" {:error-type :unknown} e))))))))

(defn delete-bookmark
  [{:bookmark/keys [id user-id] :as bookmark}]
  (if-let [err (m/explain s/bookmark-delete-spec bookmark)]
    (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Delete bookmark" id "for user" user-id)
      (jdbc/execute-one! ds (-> (helpers/delete-from :bookmark)
                                (helpers/where [:= :bookmark/id id] [:= :bookmark/user-id user-id])
                                (helpers/returning :*)
                                (sql/format))))))

(defn fetch-bookmark
  [{:bookmark/keys [id user-id] :as input}]
  (if-let [err (m/explain s/bookmark-fetch-spec input)]
    (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Fetch bookmark" id "for user" user-id)
      (jdbc/execute-one! ds (-> (helpers/select :*)
                                (helpers/from :bookmark)
                                (helpers/where [:= :bookmark/user-id user-id] [:= :bookmark/id id])
                                (sql/format))))))

(defn simplify-query
  [q]
  (letfn [(inner-reduce [acc inner-query kw]
            (reduce
              (fn [acc it]
                (let [it (-> it vals first)]
                  (if (:title it)
                    (if (string? (:title it))
                      (update-in acc [:title kw] conj (:title it))
                      (update-in acc [:title kw] #(apply conj % (string/split (get-in it [:title :query]) #" "))))
                    (if (string? (:tag it))
                      (update-in acc [:tag kw] conj (:tag it))
                      (update-in acc [:tag kw] #(apply conj % (string/split (get-in it [:tag :query]) #" ")))))))
              acc
              inner-query))
          (inner-simplify [q]
            (reduce
              (fn [acc it]
                (if-let [title-query (get-in it [:simple_query_string :query])]
                  (assoc acc :simple title-query)
                  (if-let [inner-query (get-in it [:bool :must])]
                    (inner-reduce acc inner-query :and)
                    (if-let [inner-query (get-in it [:bool :should])]
                      (inner-reduce acc inner-query :or)
                      (if-let [inner-query (get-in it [:match_phrase])]
                        (inner-reduce acc [it] :and)
                        (inner-reduce acc [it] (-> it vals first vals first :operator keyword)))))))
              {}
              q))]
    (if-not (or (nil? q) (not (map? q)) (:match_all q))
      {:must     (inner-simplify (get-in q [:bool :must]))
       :must-not (inner-simplify (get-in q [:bool :must-not]))})))

(defn- query->sql
  "Given a query after parsed by `simplify-query`, return an SQL"
  [query user-id tab-id]
  (letfn [(tag-fn [tag-name]
            [:in :bookmark-tag/tag-id (-> (helpers/select :tag/id)
                                          (helpers/from :tag)
                                          (helpers/where [:ilike :tag/name (str "%" tag-name "%")]))])
          (inner-fn [kw col]
            (if-not (empty? col)
              (into [kw]
                    (mapv
                      (fn [i]
                        [:in :bookmark-tag/bookmark-id (-> (helpers/select-distinct :bookmark-tag/bookmark-id)
                                                           (helpers/from :bookmark-tag)
                                                           (helpers/where (tag-fn i)))])
                      col))))]
    (into
      [:and]
      (reduce
        (fn [acc [_ v]]
          (reduce
            (fn [acc [k {:keys [and or] :as v}]]
              (case k
                :simple
                (conj acc [:ilike :bookmark/title (str "%" v "%")])
                :title
                (reduce
                  (fn [acc [k v]]
                    (case k
                      :and (vec (concat acc (mapv (fn [n] [:ilike :bookmark/title (str "%" n "%")]) v)))
                      :or (conj acc (into [:or] (mapv (fn [n] [:ilike :bookmark/title (str "%" n "%")]) v)))))
                  acc
                  v)
                :tag
                (conj acc [:in :bookmark/id (-> (helpers/select-distinct :bookmark-tag/bookmark-id)
                                                (helpers/from :bookmark-tag)
                                                (helpers/where (inner-fn :and and) (inner-fn :or or)))])))
            acc
            v))
        [[:= :bookmark/user-id user-id]
         [:= :bookmark/tab-id tab-id]]
        query))))

(defn fetch-bookmarks
  ([bookmark] (fetch-bookmarks bookmark nil))
  ([{:bookmark/keys [tab-id user-id] :as input}
    {:keys [query sort page size]
     :or   {sort {:field :bookmark/created :direction :asc} page 0 size 12}
     :as   opts}]
   (if-let [err (or (m/explain s/bookmark-bulk-fetch-spec input) (m/explain [:maybe s/bookmark-fetch-opts-spec] opts))]
     (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)})))
   (log/info "Fetch all bookmarks in tab" tab-id "for user" user-id)
   (jdbc/execute! ds (-> (helpers/select :bookmark/*)
                         (helpers/from :bookmark)
                         (helpers/where (-> query simplify-query (query->sql user-id tab-id)))
                         (helpers/order-by [(:field sort) (:direction sort)])
                         (helpers/limit size)
                         (helpers/offset (* page size))
                         (sql/format)))))

(comment
  (let [{:keys [a] :or {a "boo"} :as m} {:b 1}]
    a)
  (require '[mount.core :as mount])
  (mount/start)
  (into [:a] [1 2 3]))
