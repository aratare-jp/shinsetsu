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

(defn- simplify-query
  [q]
  (letfn [(inner-reduce [acc inner-query kw]
            (reduce
              (fn [acc it]
                (let [it (-> it vals first)]
                  (if (:name it)
                    (if (string? (:name it))
                      (update-in acc [:name kw] conj (:name it))
                      (update-in acc [:name kw] #(apply conj % (string/split (get-in it [:name :query]) #" "))))
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
                    (inner-reduce acc (get-in it [:bool :should]) :or))))
              {}
              q))]
    (cond
      (nil? q)
      nil
      (:match_all q)
      {:match_all true}
      :else
      {:must     (inner-simplify (get-in q [:bool :must]))
       :must-not (inner-simplify (get-in q [:bool :must-not]))})))

(defn fetch-bookmarks
  ([input] (fetch-bookmarks input nil))
  ([{:bookmark/keys [tab-id user-id] :as input} query]
   (if-let [err (m/explain s/bookmark-multi-fetch-spec input)]
     (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
     (do
       (log/info "Fetch all bookmarks in tab" tab-id "for user" user-id)
       (jdbc/execute! ds (-> (helpers/select :*)
                             (helpers/from :bookmark)
                             (helpers/where [:= :bookmark/user-id user-id]
                                            [:= :bookmark/tab-id tab-id]
                                            (if-let [query (simplify-query query)]
                                              (into [])))
                             (helpers/order-by [:bookmark/created :asc])
                             (sql/format)))))))

(comment
  (let [keywordize (fn [it]
                     (if (map? it)
                       (reduce (fn [acc [k v]] (assoc acc (keyword k) v)) {} it)
                       it))
        query      {:bool {:must     [{:simple_query_string {:query "yeet"}}
                                      {:bool {:must [{:match {:name {:query "thin buff", :operator "and"}}}
                                                     {:match_phrase {:name "fat foo"}}]}}
                                      {:bool {:must [{:match {:tag {:query "yep", :operator "and"}}}
                                                     {:match_phrase {:tag "111 222"}}
                                                     {:match_phrase {:tag "333 444"}}]}}
                                      {:bool {:should [{:match_phrase {:name "foo bar"}}
                                                       {:match_phrase {:name "hello world"}}
                                                       {:match_phrase {:name "aaa bbb"}}
                                                       {:match_phrase {:name "ccc ddd"}}]}}
                                      {:bool {:should [{:match {:tag {:query "dang world", :operator "or"}}}
                                                       {:match_phrase {:tag "hello world"}}]}}]
                           :must-not [{:simple_query_string {:query "yeet1"}}
                                      {:bool {:must [{:match {:name {:query "thin", :operator "and"}}}
                                                     {:match_phrase {:name "fat foo"}}]}}
                                      {:bool {:must [{:match {:tag {:query "yep", :operator "and"}}}
                                                     {:match_phrase {:tag "111 222"}}
                                                     {:match_phrase {:tag "333 444"}}]}}
                                      {:bool {:should [{:match_phrase {:name "foo bar"}}
                                                       {:match_phrase {:name "hello world"}}
                                                       {:match_phrase {:name "aaa bbb"}}
                                                       {:match_phrase {:name "ccc ddd"}}]}}
                                      {:bool {:should [{:match {:tag {:query "world", :operator "or"}}}
                                                       {:match_phrase {:tag "hello world"}}]}}]}}
        sql-fn     #(-> (helpers/select :*)
                        (helpers/from :bookmark)
                        (helpers/where %)
                        (helpers/order-by [:bookmark/created :asc])
                        (sql/format {:pretty true}))]
    (sql-fn
      (vec (concat
             [:and]
             (reduce
               (fn [acc [k {:keys [simple name tag] :as v}]]
                 (reduce
                   (fn [acc [k v]]
                     (case k
                       :simple
                       (conj acc [:ilike :bookmark/title (str "%" v "%")])
                       :name
                       (reduce
                         (fn [acc [k v]]
                           (case k
                             :and
                             (vec (concat acc (mapv (fn [n] [:ilike :bookmark/title (str "%" n "%")]) v)))
                             :or
                             (conj acc (vec (concat [:or] (mapv (fn [n] [:ilike :bookmark/title (str "%" n "%")]) v))))))
                         acc
                         v)
                       :tag
                       (let [{:keys [and or]} v
                             tag-fn (fn [a]
                                      [:in :bookmark-tag/tag-id (-> (helpers/select :tag/id)
                                                                    (helpers/from :tag)
                                                                    (helpers/where [:ilike :tag/name (str "%" a "%")]))])]
                         (conj acc
                               [:in
                                :bookmark/id
                                (-> (helpers/select-distinct :bookmark-tag/bookmark-id)
                                    (helpers/from :bookmark-tag)
                                    (helpers/where
                                      (vec (concat
                                             [:and]
                                             (mapv
                                               (fn [i]
                                                 [:in
                                                  :bookmark-tag/bookmark-id
                                                  (-> (helpers/select-distinct :bookmark-tag/bookmark-id)
                                                      (helpers/from :bookmark-tag)
                                                      (helpers/where (tag-fn i)))])
                                               and)))
                                      (vec (concat
                                             [:or]
                                             (mapv
                                               (fn [i]
                                                 [:in
                                                  :bookmark-tag/bookmark-id
                                                  (-> (helpers/select-distinct :bookmark-tag/bookmark-id)
                                                      (helpers/from :bookmark-tag)
                                                      (helpers/where (tag-fn i)))])
                                               or)))))]))))
                   acc
                   v))
               [[:= :bookmark/user-id "boo"]
                [:= :bookmark/tab-id "bar"]]
               (simplify-query query)))))))
