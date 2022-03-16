(ns shinsetsu.db.bookmark
  (:require
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as helpers]
    [honey.sql :as sql]
    [taoensso.timbre :as log]
    [shinsetsu.db.db :refer [ds]]
    [shinsetsu.schema :as s]
    [malli.core :as m]
    [malli.error :as me]))

(defn create-bookmark
  [{:bookmark/keys [tab-id user-id] :as bookmark}]
  (if-let [err (m/explain s/bookmark-spec bookmark)]
    (throw (ex-info "Invalid bookmark" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Create new bookmark in tab" tab-id "for user" user-id)
      (jdbc/execute-one! ds (-> (helpers/insert-into :bookmark)
                                (helpers/values [bookmark])
                                (helpers/returning :*)
                                (sql/format))))))

(defn fetch-bookmark
  [{bookmark-id :bookmark/id user-id :user/id :as input}]
  (if-let [err (m/explain [:map {:closed true} [:bookmark/id :uuid] [:user/id :uuid]] input)]
    (throw (ex-info "Invalid bookmark or user ID" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Fetch bookmark" bookmark-id "for user" user-id)
      (jdbc/execute-one! ds (-> (helpers/select :*)
                                (helpers/from :bookmark)
                                (helpers/where [:= :bookmark/user-id user-id] [:= :bookmark/id bookmark-id])
                                (sql/format))))))

(defn fetch-bookmarks
  [{tab-id :tab/id user-id :user/id :as input}]
  (if-let [err (m/explain [:map {:closed true} [:tab/id :uuid] [:user/id :uuid]] input)]
    (throw (ex-info "Invalid tab or user ID" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Fetch all bookmarks in tab" tab-id "for user" user-id)
      (jdbc/execute! ds (-> (helpers/select :*)
                            (helpers/from :bookmark)
                            (helpers/where [:= :bookmark/user-id user-id] [:= :bookmark/tab-id tab-id])
                            (sql/format))))))
