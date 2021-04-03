(ns shinsetsu.db.user
  (:require [honey.sql :as sql]
            [honey.sql.helpers :as helpers]
            [next.jdbc :as jdbc]
            [schema.core :as s]
            [shinsetsu.schemas :refer :all]
            [shinsetsu.utility :refer :all]
            [taoensso.timbre :as log]))

(s/defn read-user :- (s/maybe UserDB)
  [db :- Transactable
   {:user/keys [id]} :- {:user/id s/Uuid}]
  (log/info "Reading user with id" id)
  (jdbc/execute-one! db (-> (helpers/select :*)
                            (helpers/from :user)
                            (helpers/where [:= :user/id id])
                            (sql/format {:dialect :ansi}))))

(s/defn read-user-by-username :- (s/maybe UserDB)
  [db :- Transactable
   {:user/keys [username]} :- {:user/username Username}]
  (log/info "Reading user by username" username)
  (jdbc/execute-one! db (-> (helpers/select :*)
                            (helpers/from :user)
                            (helpers/where [:= :user/username username])
                            (sql/format {:dialect :ansi}))))

(s/defn create-user :- UserDB
  [db :- Transactable
   data :- User]
  (log/info "Creating new user with id" (:user/id data))
  (jdbc/execute-one! db (-> (helpers/insert-into :user)
                            (helpers/values [data])
                            (helpers/returning :*)
                            (sql/format {:dialect :ansi}))))

(s/defn update-user :- UserDB
  [db :- Transactable
   {:user/keys [id] :as data} :- PartialUser]
  (log/info "Updating user with id" id)
  (let [data (-> data
                 (dissoc :user/id)
                 (merge {:user/updated (now)})
                 ;; FIXME: This shouldn't be required. Checkin with HoneySQL for fix.
                 (->> (map (fn [[k v]] [(keyword (name k)) v])) (into {})))]
    (jdbc/execute-one! db (-> (helpers/update :user)
                              (helpers/set data)
                              (helpers/where [:= :user/id id])
                              (helpers/returning :*)
                              (sql/format {:dialect :ansi})))))

(s/defn delete-user :- (s/maybe UserDB)
  [db :- Transactable
   {:user/keys [id]} :- {:user/id s/Uuid}]
  (log/info "Deleting user with id" id)
  (jdbc/execute-one! db (-> (helpers/delete-from :user)
                            (helpers/where [:= :user/id id])
                            (helpers/returning :*)
                            (sql/format {:dialect :ansi}))))
