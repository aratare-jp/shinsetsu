(ns shinsetsu.db
  (:require
    [next.jdbc :as jdbc]
    [honey.sql :as sql]
    [honey.sql.helpers :as helpers]))

(def db-spec
  {:dbtype   "postgresql"
   :dbname   "shinsetsu"
   :user     "shinsetsudev"
   :password "shinsetsu"})

(def ds (jdbc/get-datasource db-spec))

(defn create-user
  [user]
  (jdbc/execute-one! ds (-> (helpers/insert-into :user)
                            (helpers/values [user])
                            (helpers/returning :*)
                            (sql/format {:dialect :ansi}))))

(defn fetch-user-by-username
  [{:user/keys [username]}]
  (jdbc/execute-one! ds (-> (helpers/select :*)
                            (helpers/from :user)
                            (helpers/where [:= :user/username username])
                            (sql/format {:dialect :ansi}))))

(comment
  (user/restart)
  (clojure.repl/doc tap>)
  (tap> 2)
  (def mock-user [{:username "test1" :password "test2"}])
  (-> (helpers/insert-into :user)
      (helpers/values mock-user)
      (helpers/returning :*)
      sql/format)
  (create-user mock-user)
  (fetch-user-by-username {:user/username "foo"}))
