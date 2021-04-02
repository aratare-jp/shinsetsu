(ns shinsetsu.db.user
  (:require [schema.core :as s]
            [shinsetsu.schemas :refer :all]
            [next.jdbc :as nj]
            [honey.sql.helpers :as helpers]
            [honey.sql :as sql]
            [puget.printer :refer [pprint]]
            [shinsetsu.db.core :refer :all]
            [taoensso.timbre :as log]
            [shinsetsu.utility :refer :all]
            [schema-generators.generators :as g]))

(s/defn read-user :- (s/maybe UserDB)
  [{:user/keys [id]} :- {:user/id s/Uuid}]
  (log/info "Reading user with id" id)
  (with-tx-execute-one! (-> (helpers/select :*)
                            (helpers/from :user)
                            (helpers/where [:= :user/id id])
                            (sql/format {:dialect :ansi}))))

(s/defn read-user-by-username :- (s/maybe UserDB)
  [{:user/keys [username]} :- {:user/username Username}]
  (log/info "Reading user by username" username)
  (with-tx-execute-one! (-> (helpers/select :*)
                            (helpers/from :user)
                            (helpers/where [:= :user/username username])
                            (sql/format {:dialect :ansi}))))

(s/defn create-user :- UserDB
  [data :- User]
  (log/info "Creating new user with id" (:user/id data))
  (with-tx-execute-one! (-> (helpers/insert-into :user)
                            (helpers/values [data])
                            (helpers/returning :*)
                            (sql/format {:dialect :ansi}))))

(s/defn update-user :- UserDB
  [{:user/keys [id] :as data} :- PartialUser]
  (log/info "Updating user with id" id)
  (let [data (-> data
                 (dissoc :user/id)
                 (merge {:user/updated (now)})
                 ;; FIXME: This shouldn't be required. Checkin with HoneySQL for fix.
                 (->> (map (fn [[k v]] [(keyword (name k)) v])) (into {})))]
    (with-tx-execute-one! (-> (helpers/update :user)
                              (helpers/set data)
                              (helpers/where [:= :user/id id])
                              (helpers/returning :*)
                              (sql/format {:dialect :ansi})))))

(s/defn delete-user :- (s/maybe UserDB)
  [{:user/keys [id]} :- {:user/id s/Uuid}]
  (log/info "Deleting user with id" id)
  (with-tx-execute-one! (-> (helpers/delete-from :user)
                            (helpers/where [:= :user/id id])
                            (helpers/returning :*)
                            (sql/format {:dialect :ansi}))))
