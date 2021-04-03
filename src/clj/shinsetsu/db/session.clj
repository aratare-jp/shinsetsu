(ns shinsetsu.db.session
  (:require [schema.core :as s]
            [shinsetsu.schemas :refer :all]
            [honey.sql.helpers :as helpers]
            [honey.sql :as sql]
            [taoensso.timbre :as log]
            [shinsetsu.utility :refer :all]
            [next.jdbc :as jdbc]))

(s/defn read-session :- [SessionDB]
  [db :- Transactable
   {:session/keys [user-id]} :- {:session/user-id s/Uuid}]
  (log/info "Read session with user id" user-id)
  (jdbc/execute-one! db (-> (helpers/select :*)
                            (helpers/from [:session])
                            (helpers/where [:= :user-id user-id])
                            (sql/format))))

(s/defn check-session :- SessionDB
  [db :- Transactable
   {:session/keys [user-id token]} :- Session]
  (log/info "Read session with user id" user-id)
  (jdbc/execute-one! db (-> (helpers/select :*)
                            (helpers/from [:session])
                            (helpers/where [:= :user-id user-id] [:= :token token])
                            (sql/format))))

(s/defn create-session :- SessionDB
  [db :- Transactable
   data :- Session]
  (log/info "Creating a current user with id" (:user/id data))
  (jdbc/execute-one! db (-> (helpers/insert-into :session)
                            (helpers/values [data])
                            (helpers/returning :*)
                            (sql/format))))

(s/defn delete-session :- SessionDB
  [db :- Transactable
   {:session/keys [user-id token]} :- Session]
  (log/info "Deleting current user with id" user-id)
  (jdbc/execute-one! db (-> (helpers/delete-from :session)
                            (helpers/where [:= :id user-id] [:= :token token])
                            (helpers/returning :*)
                            (sql/format))))
