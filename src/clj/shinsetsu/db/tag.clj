(ns shinsetsu.db.tag
  (:require
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as helpers]
    [honey.sql :as sql]
    [taoensso.timbre :as log]
    [shinsetsu.db :refer [ds]]
    [shinsetsu.schema :as s]
    [malli.core :as m]
    [malli.error :as me])
  (:import [java.time Instant]))

(defn create-tag
  [{:tag/keys [user-id] :as tag}]
  (if-let [err (m/explain s/tag-create-spec tag)]
    (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Create new tag for user" user-id)
      (jdbc/execute-one! ds (-> (helpers/insert-into :tag)
                                (helpers/values [tag])
                                (helpers/returning :*)
                                (sql/format))))))

(defn patch-tag
  [{:tag/keys [id user-id] :as tag}]
  (if-let [err (m/explain s/tag-update-spec tag)]
    (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
    (let [tag (assoc tag :tag/updated (Instant/now))]
      (log/info "Update tag" id "for user" user-id)
      (jdbc/execute-one! ds (-> (helpers/update :tag)
                                (helpers/set tag)
                                (helpers/where [:= :tag/id id] [:= :tag/user-id user-id])
                                (helpers/returning :*)
                                (sql/format))))))

(defn delete-tag
  [{:tag/keys [id user-id] :as tag}]
  (if-let [err (m/explain s/tag-delete-spec tag)]
    (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Delete tag" id "for user" user-id)
      (jdbc/execute-one! ds (-> (helpers/delete-from :tag)
                                (helpers/where [:= :tag/id id] [:= :tag/user-id user-id])
                                (helpers/returning :*)
                                (sql/format))))))

(defn fetch-tag
  [{:tag/keys [id user-id] :as input}]
  (if-let [err (m/explain s/tag-fetch-spec input)]
    (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Fetch tag" id "for user" user-id)
      (jdbc/execute-one! ds (-> (helpers/select :*)
                                (helpers/from :tag)
                                (helpers/where [:= :tag/user-id user-id] [:= :tag/id id])
                                (sql/format))))))

(defn fetch-tags
  [{:tag/keys [name user-id name-pos] :as input}]
  (if-let [err (m/explain s/tag-multi-fetch-spec input)]
    (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)})))
  (let [name-query (case name-pos
                     :start (str name "%")
                     :end (str "%" name)
                     (str "%" name "%"))]
    (log/info "Fetch all tags for user" user-id)
    (jdbc/execute! ds (-> (helpers/select :*)
                          (helpers/from :tag)
                          (helpers/where [:= :tag/user-id user-id]
                                         [:like :tag/name name-query])
                          (helpers/order-by [:tag/created :asc])
                          (sql/format)))))

(defn search-tags
  [{user-id :user/id :as input}]
  (if-let [err (m/explain [:map {:closed true} [:user/id :uuid]] input)]
    (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Fetch all tags for user" user-id)
      (jdbc/execute! ds (-> (helpers/select :*)
                            (helpers/from :tag)
                            (helpers/where [:= :tag/user-id user-id])
                            (helpers/order-by [:tag/created :asc])
                            (sql/format))))))
