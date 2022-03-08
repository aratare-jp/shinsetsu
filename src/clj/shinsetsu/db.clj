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
  (jdbc/execute-one! (-> (helpers/insert-into :users)
                         (helpers/values user)
                         (sql/format {:pretty true}))))
