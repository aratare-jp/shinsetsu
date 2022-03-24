(ns shinsetsu.db.bookmark
  (:require
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as helpers]
    [honey.sql :as sql]
    [taoensso.timbre :as log]
    [shinsetsu.db.db :refer [ds]]
    [shinsetsu.schema :as s]
    [malli.core :as m]
    [malli.error :as me])
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

(defn fetch-bookmarks
  [{:bookmark/keys [tab-id user-id] :as input}]
  (if-let [err (m/explain s/bookmark-multi-fetch-spec input)]
    (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Fetch all bookmarks in tab" tab-id "for user" user-id)
      (jdbc/execute! ds (-> (helpers/select :*)
                            (helpers/from :bookmark)
                            (helpers/where [:= :bookmark/user-id user-id] [:= :bookmark/tab-id tab-id])
                            (sql/format))))))

