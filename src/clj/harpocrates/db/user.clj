(ns harpocrates.db.user
  (:require [schema.core :as s]
            [harpocrates.spec :as hs]
            [harpocrates.db.core :refer [db crud-fns]]
            [next.jdbc :as nj]
            [honeysql.helpers :as helpers]
            [honeysql.core :as sql]
            [buddy.hashers :as hashers]
            [harpocrates.db.tab :refer [Tab]]))

(def User
  {:user/id       s/Uuid
   :user/username hs/NonEmptyContinuousStr
   :user/password hs/NonEmptyContinuousStr
   :user/created  s/Inst
   :user/updated  s/Inst})

(def CurrentUser
  {:user/id      s/Uuid
   :user/token   hs/NonEmptyContinuousStr
   :user/created s/Inst
   :user/updated s/Inst})

(def User? (into {} (map (fn [[k v]] [(s/optional-key ~k) (s/maybe v)]) User)))

(s/defn read-user :- User
  [{:user/keys [id]} :- {:user/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/select :*)
                            (helpers/from :user)
                            (helpers/where [:= :user/id id])
                            (sql/format :quoting :ansi)))))

(s/defn read-user-by-username :- User
  [{:user/keys [username]} :- (:username User)]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/select :*)
                            (helpers/from :user)
                            (helpers/where [:= :user/username username])
                            (sql/format :quoting :ansi)))))

(s/defn create-user :- User
  [data :- User]
  (let [data (update data :password hashers/derive)]
    (nj/with-transaction [tx db]
      (nj/execute-one! tx (-> (helpers/insert-into :user)
                              (helpers/values [data])
                              (sql/format :quoting :ansi))))
    data))

(s/defn update-user :- User?
  [{:user/keys [id] :as data} :- User?]
  (let [data (if (:password data) (update data :password hashers/derive) data)]
    (nj/with-transaction [tx db]
      (nj/execute-one! tx (-> (helpers/update :user)
                              (helpers/sset data)
                              (helpers/where [:= :user/id ~id])
                              (sql/format :quoting :ansi))))
    data))

(s/defn delete-user :- User
  [{:user/keys [id]} :- {:user/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/delete-from :user)
                            (helpers/where [:= :user/id id])
                            (sql/format :quoting :ansi)))))

(s/defn read-current-user :- [CurrentUser]
  [{:user/keys [id]} :- {:user/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute! tx (-> (helpers/select :*)
                        (helpers/from :current-user)
                        (helpers/where [:= :user-id id])
                        (sql/format :quoting :ansi)))))

(s/defn create-current-user :- CurrentUser
  [data :- CurrentUser]
  (let [data (update data :password hashers/derive)]
    (nj/with-transaction [tx db]
      (nj/execute-one! tx (-> (helpers/insert-into :current-user)
                              (helpers/values [data])
                              (sql/format :quoting :ansi))))
    data))

(s/defn delete-current-user :- CurrentUser
  [{:user/keys [id token]} :- CurrentUser]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/delete-from :current-user)
                            (helpers/where [:= :user/id id] [:= :user/token token])
                            (sql/format :quoting :ansi)))))

(s/defn read-user-tab :- [Tab]
  [{:user/keys [id]} :- {:user/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/select :tab)
                            (helpers/where [:= :tab/user-id id])
                            (sql/format :quoting :ansi)))))
