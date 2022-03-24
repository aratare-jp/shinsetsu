(ns shinsetsu.db.bookmark-tag
  (:require [malli.core :as m]
            [shinsetsu.schema :as s]
            [malli.error :as me]
            [taoensso.timbre :as log]
            [next.jdbc :as jdbc]
            [honey.sql.helpers :as helpers]
            [honey.sql :as sql]
            [shinsetsu.db.db :refer [ds]])
  (:import [org.postgresql.util PSQLException]))

(defn create-bookmark-tag
  [{:bookmark-tag/keys [bookmark-id tag-id user-id] :as input}]
  (if-let [err (m/explain s/bookmark-tag-create-spec input)]
    (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
    (try
      (log/info "Assign tag" tag-id "to bookmark" bookmark-id "for user" user-id)
      (jdbc/execute-one! ds (-> (helpers/insert-into :bookmark-tag)
                                (helpers/values [{:bookmark-tag/bookmark-id bookmark-id
                                                  :bookmark-tag/tag-id      tag-id
                                                  :bookmark-tag/user-id     user-id}])
                                (helpers/returning :*)
                                (sql/format)))
      (catch PSQLException e
        (log/error e)
        (case (.getSQLState e)
          "23503" (throw (ex-info "Invalid input" {:error-type :invalid-input
                                                   :error-data {:bookmark-tag/bookmark-id ["nonexistent"]
                                                                :bookmark-tag/tag-id      ["nonexistent"]}}))
          (throw (ex-info "Unknown error" {:error-type :unknown} e)))))))

(defn fetch-tags-by-bookmark
  [{:bookmark-tag/keys [bookmark-id user-id] :as input}]
  (if-let [err (m/explain s/bookmark-tag-fetch-by-bookmark-spec input)]
    (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Fetch tags of bookmark" bookmark-id "for user" user-id)
      (jdbc/execute! ds (-> (helpers/select :*)
                            (helpers/from :bookmark-tag)
                            (helpers/where [:= :bookmark-tag/bookmark-id bookmark-id]
                                           [:= :bookmark-tag/user-id user-id])
                            (sql/format))))))

(defn fetch-bookmarks-by-tag
  [{:bookmark-tag/keys [tag-id user-id] :as input}]
  (if-let [err (m/explain s/bookmark-tag-fetch-by-tag-spec input)]
    (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Fetch bookmarks that have tag" tag-id "for user" user-id)
      (jdbc/execute! ds (-> (helpers/select :*)
                            (helpers/from :bookmark-tag)
                            (helpers/where [:= :bookmark-tag/tag-id tag-id]
                                           [:= :bookmark-tag/user-id user-id])
                            (sql/format))))))

(defn delete-bookmark-tag
  [{:bookmark-tag/keys [bookmark-id tag-id user-id] :as input}]
  (if-let [err (m/explain s/bookmark-tag-delete-spec input)]
    (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Delete tag" tag-id "from bookmark" bookmark-id "for user" user-id)
      (jdbc/execute-one! ds (-> (helpers/delete-from :bookmark-tag)
                                (helpers/where [:= :bookmark-tag/bookmark-id bookmark-id]
                                               [:= :bookmark-tag/tag-id tag-id]
                                               [:= :bookmark-tag/user-id user-id])
                                (helpers/returning :*)
                                (sql/format))))))
