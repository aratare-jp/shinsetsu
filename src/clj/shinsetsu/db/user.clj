(ns shinsetsu.db.user
  (:require
    [next.jdbc :as jdbc]
    [honey.sql :as sql]
    [honey.sql.helpers :as helpers]
    [taoensso.timbre :as log]
    [shinsetsu.db.db :refer [ds]]
    [shinsetsu.schema :as s]
    [malli.core :as m]
    [malli.error :as me])
  (:import [java.time Instant]))

(defn create-user
  [{:user/keys [username] :as user}]
  (if-let [err (m/explain s/user-spec user)]
    (throw (ex-info "Invalid user" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Create a new user with username" username)
      (jdbc/execute-one! ds (-> (helpers/insert-into :user)
                                (helpers/values [user])
                                (helpers/returning :*)
                                (sql/format {:dialect :ansi}))))))

(defn patch-user
  [{:user/keys [id username] :as user}]
  (if-let [err (m/explain s/user-update-spec user)]
    (throw (ex-info "Invalid user" {:error-type :invalid-input :error-data (me/humanize err)}))
    (let [user (assoc user :user/updated (Instant/now))]
      (do
        (log/info "Patching user with ID" id)
        (jdbc/execute-one! ds (-> (helpers/update :user)
                                  (helpers/set user)
                                  (helpers/where [:= :user/id id])
                                  (helpers/returning :*)
                                  (sql/format {:dialect :ansi})))))))

(defn fetch-user-by-username
  [{:user/keys [username] :as input}]
  (if-let [err (m/explain [:map {:closed true} [:user/username s/non-empty-string]] input)]
    (throw (ex-info "Invalid username" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Fetching user with username" username)
      (jdbc/execute-one! ds (-> (helpers/select :*)
                                (helpers/from :user)
                                (helpers/where [:= :user/username username])
                                (sql/format {:dialect :ansi}))))))

(defn fetch-user-by-id
  [{:user/keys [id] :as input}]
  (if-let [err (m/explain [:map {:closed true} [:user/id :uuid]] input)]
    (throw (ex-info "Invalid user ID" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Fetching user with ID" id)
      (jdbc/execute-one! ds (-> (helpers/select :*)
                                (helpers/from :user)
                                (helpers/where [:= :user/id id])
                                (sql/format {:dialect :ansi}))))))
