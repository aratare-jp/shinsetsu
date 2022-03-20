(ns shinsetsu.db.tab
  (:require
    [shinsetsu.db.db :refer [ds]]
    [shinsetsu.db.bookmark :as bookmark-db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as helpers]
    [honey.sql :as sql]
    [taoensso.timbre :as log]
    [shinsetsu.schema :as s]
    [malli.error :as me]
    [malli.core :as m])
  (:import [java.time Instant]))

(defn create-tab
  [{:tab/keys [user-id] :as tab}]
  (if-let [err (m/explain s/tab-spec tab)]
    (throw (ex-info "Invalid tab" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Create a new tab for user" user-id)
      (jdbc/execute-one! ds (-> (helpers/insert-into :tab)
                                (helpers/values [tab])
                                (helpers/returning :*)
                                (sql/format))))))

(defn patch-tab
  [{:tab/keys [id user-id] :as tab}]
  (if-let [err (m/explain s/tab-update-spec tab)]
    (throw (ex-info "Invalid tab" {:error-type :invalid-input :error-data (me/humanize err)}))
    (let [tab (assoc tab :tab/updated (Instant/now))]
      (do
        (log/info "Patching tab with ID" id)
        (jdbc/execute-one! ds (-> (helpers/update :tab)
                                  (helpers/set tab)
                                  (helpers/where [:= :tab/id id] [:= :tab/user-id user-id])
                                  (helpers/returning :*)
                                  (sql/format)))))))

(defn delete-tab
  [{:tab/keys [id user-id] :as tab}]
  (if-let [err (m/explain s/tab-delete-spec tab)]
    (throw (ex-info "Invalid tab" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Deleting tab with ID" id)
      (jdbc/execute-one! ds (-> (helpers/delete-from :tab)
                                (helpers/where [:= :tab/id id] [:= :tab/user-id user-id])
                                (helpers/returning :*)
                                (sql/format))))))

(defn fetch-tabs
  [{user-id :user/id :as input}]
  (if-let [err (m/explain [:map {:closed true} [:user/id :uuid]] input)]
    (throw (ex-info "Invalid user ID" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Fetch tabs for user" user-id)
      (jdbc/execute! ds (-> (helpers/select :*)
                            (helpers/from :tab)
                            (helpers/where [:= :tab/user-id user-id])
                            (sql/format))))))

(defn fetch-tab
  [{tab-id :tab/id user-id :user/id :as input}]
  (if-let [err (m/explain [:map {:closed true} [:tab/id :uuid] [:user/id :uuid]] input)]
    (throw (ex-info "Invalid user or tab ID" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Fetch tab" tab-id "for user" user-id)
      (jdbc/execute-one! ds (-> (helpers/select :*)
                                (helpers/from :tab)
                                (helpers/where [:= :tab/user-id user-id] [:= :tab/id tab-id])
                                (sql/format))))))
