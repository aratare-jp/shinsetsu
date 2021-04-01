(ns shinsetsu.db.user
  (:require [schema.core :as s]
            [shinsetsu.schemas :refer :all]
            [next.jdbc :as nj]
            [honey.sql.helpers :as helpers]
            [honey.sql :as sql]
            [puget.printer :refer [pprint]]
            [shinsetsu.db.core :refer [db]]
            [taoensso.timbre :as log]
            [shinsetsu.utility :refer :all]
            [schema-generators.generators :as g]))

(s/defn read-user :- (s/maybe UserDB)
  [{:user/keys [id]} :- {:user/id s/Uuid}]
  (log/info "Reading user with id" id)
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/select :*)
                            (helpers/from :user)
                            (helpers/where [:= :user/id id])
                            (sql/format {:dialect :ansi})))))

(s/defn read-user-by-username :- (s/maybe UserDB)
  [{:user/keys [username]} :- {:user/username Username}]
  (log/info "Reading user by username" username)
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/select :*)
                            (helpers/from :user)
                            (helpers/where [:= :user/username username])
                            (sql/format {:dialect :ansi})))))

(s/defn create-user :- UserDB
  [data :- User]
  (log/info "Creating new user with id" (:user/id data))
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/insert-into :user)
                            (helpers/values [data])
                            (helpers/returning :*)
                            (sql/format {:dialect :ansi})))))

(s/defn update-user :- UserDB
  [{:user/keys [id] :as data} :- PartialUser]
  (log/info "Updating user with id" id)
  (let [data (-> data
                 (dissoc :user/id)
                 (merge {:user/updated (now)})
                 ;; FIXME: This shouldn't be required. Checkin with HoneySQL for fix.
                 (->> (map (fn [[k v]] [(keyword (name k)) v])) (into {})))]
    (nj/with-transaction [tx db]
      (nj/execute-one! tx (-> (helpers/update :user)
                              (helpers/set data)
                              (helpers/where [:= :user/id id])
                              (helpers/returning :*)
                              (sql/format {:dialect :ansi}))))))

(s/defn delete-user :- (s/maybe UserDB)
  [{:user/keys [id]} :- {:user/id s/Uuid}]
  (log/info "Deleting user with id" id)
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/delete-from :user)
                            (helpers/where [:= :user/id id])
                            (helpers/returning :*)
                            (sql/format {:dialect :ansi})))))

(s/defn check-current-user :- s/Bool
  "Check if the given user id and token is still valid, i.e. the token is still operational."
  [{:user/keys [id token]} :- CurrentUser]
  (log/info "Check current user with id" id)
  (boolean (nj/with-transaction [tx db]
             (nj/execute-one! tx (-> (helpers/select :*)
                                     (helpers/from [:current_user :user])
                                     (helpers/where [:= :user/id id] [:= :user/token token])
                                     (sql/format {:dialect :ansi}))))))

(s/defn create-current-user :- CurrentUserDB
  [data :- CurrentUser]
  (log/info "Creating a current user with id" (:user/id data))
  (simplify-kw
    (nj/with-transaction [tx db]
      (nj/execute-one! tx (-> (helpers/insert-into :current_user)
                              (helpers/values [data])
                              (helpers/returning :*)
                              (sql/format {:dialect :ansi}))))
    "user"))

(s/defn delete-current-user :- CurrentUserDB
  [{:user/keys [id token]} :- CurrentUser]
  (log/info "Deleting current user with id" id)
  (simplify-kw
    (nj/with-transaction [tx db]
      (nj/execute-one! tx (-> (helpers/delete-from :current_user :user)
                              (helpers/where [:= :user/id id] [:= :user/token token])
                              (helpers/returning :*)
                              (sql/format {:dialect :ansi}))))
    "user"))

(comment
  (require '[schema-generators.generators :as g])
  (require '[shinsetsu.utility :refer :all])
  (check-current-user {:user/id    (java.util.UUID/fromString "c47e616e-ab5d-44d9-8d0c-e066646de41c")
                       :user/token "foo"})
  (delete-current-user {:user/id    (java.util.UUID/fromString "c47e616e-ab5d-44d9-8d0c-e066646de41c")
                        :user/token "foo"})
  (create-user (g/generate User default-leaf-generator))
  (create-current-user {:user/id    (java.util.UUID/fromString "c47e616e-ab5d-44d9-8d0c-e066646de41c")
                        :user/token "baz"}))