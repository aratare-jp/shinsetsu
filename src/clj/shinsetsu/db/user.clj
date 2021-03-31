(ns shinsetsu.db.user
  (:require [schema.core :as s]
            [shinsetsu.schemas :refer :all]
            [next.jdbc :as nj]
            [honey.sql.helpers :as helpers]
            [honey.sql :as sql]
            [shinsetsu.db.core :refer [db builder-fn]]
            [taoensso.timbre :as log]))

(s/defn read-user :- (s/maybe User)
  [{:user/keys [id]} :- {:user/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/select :*)
                            (helpers/from :user)
                            (helpers/where [:= :user/id id])
                            (sql/format {:dialect :ansi}))
                     {:builder-fn builder-fn})))

(s/defn read-user-by-username :- (s/maybe User)
  [{:user/keys [username]} :- (:username User)]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/select :*)
                            (helpers/from :user)
                            (helpers/where [:= :user/username username])
                            (sql/format {:dialect :ansi})))))

(s/defn create-user :- User
  [data :- User]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/insert-into :user)
                            (helpers/values [data])
                            (helpers/returning :*)
                            (sql/format {:dialect :ansi}))
                     {:builder-fn builder-fn})))

(s/defn update-user :- User
  [{:user/keys [id] :as data} :- User?]
  (let [data (dissoc data :user/id)]
    (nj/with-transaction [tx db]
      (nj/execute-one! tx (log/spy :info (-> (helpers/update :user)
                                             (helpers/set data)
                                             (helpers/where [:= :user/id id])
                                             (helpers/returning :*)
                                             (sql/format {:dialect :ansi})))))))

(s/defn delete-user :- (s/maybe User)
  [{:user/keys [id]} :- {:user/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/delete-from :user)
                            (helpers/where [:= :user/id id])
                            (helpers/returning :*)
                            (sql/format {:dialect :ansi})))))

(s/defn read-current-user :- [CurrentUser]
  [{:user/keys [id]} :- {:user/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute! tx (-> (helpers/select :*)
                        (helpers/from :current-user)
                        (helpers/where [:= :user-id id])
                        (sql/format {:dialect :ansi})))))

(s/defn create-current-user :- CurrentUser
  [data :- CurrentUser]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/insert-into :current-user)
                            (helpers/values [data])
                            (helpers/returning :*)
                            (sql/format {:dialect :ansi})))))

(s/defn delete-current-user :- CurrentUser
  [{:user/keys [id token]} :- CurrentUser]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/delete-from :current-user)
                            (helpers/where [:= :user/id id] [:= :user/token token])
                            (helpers/returning :*)
                            (sql/format {:dialect :ansi})))))

(s/defn read-user-tab :- [Tab]
  [{:user/keys [id]} :- {:user/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/select :tab)
                            (helpers/where [:= :tab/user-id id])
                            (sql/format {:dialect :ansi})))))
