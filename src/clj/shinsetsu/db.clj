(ns shinsetsu.db
  (:require
    [next.jdbc :as jdbc]
    [honey.sql :as sql]
    [honey.sql.helpers :as helpers]
    [taoensso.timbre :as log]))

(def db-spec
  {:dbtype   "postgresql"
   :dbname   "shinsetsu"
   :user     "shinsetsudev"
   :password "shinsetsu"})

(def ds (jdbc/get-datasource db-spec))

(defn create-user
  [user]
  (try
    (jdbc/execute-one! ds (-> (helpers/insert-into :user)
                              (helpers/values [user])
                              (helpers/returning :*)
                              (sql/format {:dialect :ansi})))
    (catch Exception e
      (log/error (.getStackTrace e)))))

(defn fetch-user-by-username
  [{:user/keys [username]}]
  (log/info "Fetching user with username" username)
  (try
    (jdbc/execute-one! ds (-> (helpers/select :*)
                              (helpers/from :user)
                              (helpers/where [:= :user/username username])
                              (sql/format {:dialect :ansi})))
    (catch Exception e
      (log/error (.getStackTrace e)))))

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
