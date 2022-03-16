(ns shinsetsu.test-utility
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

(def migratus-config (atom nil))

(defn db-setup
  [f]
  (let [container   (-> (tc/create {:image-name    "postgres"
                                    :exposed-ports [5432]
                                    :env-vars      {"POSTGRES_DB"       db-name
                                                    "POSTGRES_USER"     db-username
                                                    "POSTGRES_PASSWORD" db-password}})
                        (tc/bind-filesystem! {:host-path      "/tmp"
                                              :container-path "/opt"
                                              :mode           :read-only})
                        (tc/start!))
        mapped-port (-> container :mapped-ports (get 5432))
        db-spec     {:dbtype   "postgresql"
                     :port     mapped-port
                     :dbname   db-name
                     :user     db-username
                     :password db-password}]
    (reset! migratus-config {:store         :database
                             :migration-dir "migrations"
                             :db            {:classname   "org.postgresql.driver"
                                             :subprotocol "postgresql"
                                             :subname     (str "//localhost:" mapped-port "/" db-name)
                                             :user        db-username
                                             :password    db-password}})
    (mount/start-with {#'shinsetsu.db.db/ds (jdbc/with-options
                                              (jdbc/get-datasource db-spec)
                                              jdbc/snake-kebab-opts)})
    (f)
    (mount/stop #'shinsetsu.db.db/ds)
    (tc/stop! container)))

(defn db-cleanup
  [f]
  ;; Sleep here to wait for the db to get setup properly. Although this should be done in a loop.
  (Thread/sleep 1000)
  (migratus/reset @migratus-config)
  (f))
