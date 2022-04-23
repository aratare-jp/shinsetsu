(ns shinsetsu.db.tab
  (:require
    [honey.sql :as sql]
    [honey.sql.helpers :as helpers]
    [malli.core :as m]
    [malli.error :as me]
    [next.jdbc :as jdbc]
    [shinsetsu.db :refer [ds]]
    [shinsetsu.schema :as s]
    [taoensso.timbre :as log])
  (:import [java.time Instant]))

(defn create-tab
  [{:tab/keys [user-id] :as input}]
  (if-let [err (m/explain s/tab-create-spec input)]
    (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Create a new tab for user" user-id)
      (jdbc/execute-one! ds (-> (helpers/insert-into :tab)
                                (helpers/values [input])
                                (helpers/returning :*)
                                (sql/format :dialect :ansi))))))

(defn fetch-tab
  [{:tab/keys [id user-id] :as input}]
  (if-let [err (m/explain s/tab-fetch-spec input)]
    (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Fetch tab" id "for user" user-id)
      (jdbc/execute-one! ds (-> (helpers/select :*)
                                (helpers/from :tab)
                                (helpers/where [:= :tab/user-id user-id] [:= :tab/id id])
                                (sql/format :dialect :ansi))))))

(defn fetch-tabs
  [{:tab/keys [user-id] :as input}]
  (if-let [err (m/explain s/tab-multi-fetch-spec input)]
    (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Fetch tabs for user" user-id)
      (jdbc/execute! ds (-> (helpers/select :*)
                            (helpers/from :tab)
                            (helpers/where [:= :tab/user-id user-id])
                            (helpers/order-by [:tab/created :asc])
                            (sql/format :dialect :ansi))))))

(defn patch-tab
  [{:tab/keys [id user-id] :as input}]
  (if-let [err (m/explain s/tab-patch-spec input)]
    (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
    (let [tab (assoc input :tab/updated (Instant/now))]
      (log/info "Patching tab with ID" id)
      (jdbc/execute-one! ds (-> (helpers/update :tab)
                                (helpers/set tab)
                                (helpers/where [:= :tab/id id] [:= :tab/user-id user-id])
                                (helpers/returning :*)
                                (sql/format :dialect :ansi))))))

(defn delete-tab
  [{:tab/keys [id user-id] :as input}]
  (if-let [err (m/explain s/tab-delete-spec input)]
    (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Deleting tab with ID" id)
      (jdbc/execute-one! ds (-> (helpers/delete-from :tab)
                                (helpers/where [:= :tab/id id] [:= :tab/user-id user-id])
                                (helpers/returning :*)
                                (sql/format :dialect :ansi))))))
