(ns shinsetsu.db.test-utility
  (:require
    [clj-test-containers.core :as tc]
    [mount.core :as mount]
    [next.jdbc :as jdbc]
    [shinsetsu.db.db]
    [migratus.core :as migratus]
    [taoensso.timbre :as log]))

(def db-name "shinsetsu_test")
(def db-username "shinsetsu")
(def db-password "shinsetsu")

(defn db-setup
  [f]
  (let [container       (-> (tc/create {:image-name    "postgres"
                                        :exposed-ports [5432]
                                        :env-vars      {"POSTGRES_DB"       db-name
                                                        "POSTGRES_USER"     db-username
                                                        "POSTGRES_PASSWORD" db-password}})
                            (tc/bind-filesystem! {:host-path      "/tmp"
                                                  :container-path "/opt"
                                                  :mode           :read-only})
                            (tc/start!))
        mapped-port     (-> container :mapped-ports (get 5432))
        db-spec         {:dbtype   "postgresql"
                         :port     mapped-port
                         :dbname   db-name
                         :user     db-username
                         :password db-password}
        migratus-config {:store         :database
                         :migration-dir "migrations"
                         :db            {:classname   "org.postgresql.driver"
                                         :subprotocol "postgresql"
                                         :subname     (str "//localhost:" mapped-port "/" db-name)
                                         :user        db-username
                                         :password    db-password}}]
    (log/info "Migrating database")
    (migratus/migrate migratus-config)
    (mount/start-with {#'shinsetsu.db.db/ds (jdbc/get-datasource db-spec)})
    (f)
    (mount/stop)
    (tc/stop! container)))
