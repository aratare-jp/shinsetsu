{:store         :database
 :migration-dir "migrations"
 :db            {:classname   "org.postgresql.driver"
                 :subprotocol "postgresql"
                 :subname     (or (get (System/getenv) "DB_SPEC__SUBNAME") "//localhost:5432/shinsetsu")
                 :user        (or (get (System/getenv) "DB_SPEC__USER") "shinsetsudev")
                 :password    (or (get (System/getenv) "DB_SPEC__PASSWORD") "shinsetsu")}}
