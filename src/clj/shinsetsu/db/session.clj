(ns shinsetsu.db.session
  (:require [schema.core :as s]
            [shinsetsu.schemas :refer :all]
            [honey.sql.helpers :as helpers]
            [honey.sql :as sql]
            [puget.printer :refer [pprint]]
            [shinsetsu.db.core :refer :all]
            [taoensso.timbre :as log]
            [shinsetsu.utility :refer :all]))

(s/defn read-session :- [SessionDB]
  [{:session/keys [user-id]} :- {:session/user-id s/Uuid}]
  (log/info "Read session with user id" user-id)
  (with-tx-execute! (-> (helpers/select :*)
                        (helpers/from [:session])
                        (helpers/where [:= :user-id user-id])
                        (sql/format))))

(s/defn check-session :- SessionDB
  [{:session/keys [user-id token]} :- Session]
  (log/info "Read session with user id" user-id)
  (with-tx-execute! (-> (helpers/select :*)
                        (helpers/from [:session])
                        (helpers/where [:= :user-id user-id] [:= :token token])
                        (sql/format))))

(s/defn create-session :- SessionDB
  [data :- Session]
  (log/info "Creating a current user with id" (:user/id data))
  (with-tx-execute-one! (-> (helpers/insert-into :session)
                            (helpers/values [data])
                            (helpers/returning :*)
                            (sql/format))))

(s/defn delete-session :- SessionDB
  [{:session/keys [user-id token]} :- Session]
  (log/info "Deleting current user with id" user-id)
  (with-tx-execute-one! (-> (helpers/delete-from :session)
                            (helpers/where [:= :id user-id] [:= :token token])
                            (helpers/returning :*)
                            (sql/format))))
