(ns shinsetsu.test-utility
  (:require
    [clj-test-containers.core :as tc]
    [mount.core :as mount]
    [next.jdbc :as jdbc]
    [shinsetsu.db.db :as db]
    [migratus.core :as migratus]
    [kaocha.hierarchy :as kh]
    [taoensso.timbre :as log]))

(def db-name "shinsetsu_test")
(def db-username "shinsetsu")
(def db-password "shinsetsu")
(def migratus-config (atom nil))
(def db-container (atom nil))

(defn db-container-setup
  [testable test-plan]
  (if (and (kh/suite? testable)
           (not (:skip-db testable))
           (not (:kaocha.testable/skip testable))
           (nil? @db-container))
    (let [container   (reset! db-container (-> (tc/create {:image-name    "postgres"
                                                           :exposed-ports [5432]
                                                           :env-vars      {"POSTGRES_DB"       db-name
                                                                           "POSTGRES_USER"     db-username
                                                                           "POSTGRES_PASSWORD" db-password}})
                                               (tc/bind-filesystem! {:host-path      "/tmp"
                                                                     :container-path "/opt"
                                                                     :mode           :read-only})
                                               (tc/start!)))
          mapped-port (-> container :mapped-ports (get 5432))]
      (reset! migratus-config {:store         :database
                               :migration-dir "migrations"
                               :db            {:classname   "org.postgresql.driver"
                                               :subprotocol "postgresql"
                                               :subname     (str "//localhost:" mapped-port "/" db-name)
                                               :user        db-username
                                               :password    db-password}})))
  testable)

(defn db-container-teardown
  [testable test-plan]
  (if (and (kh/suite? testable)
           (not (:skip-db testable))
           (not (:kaocha.testable/skip testable))
           (nil? @db-container))
    (do
      (tc/stop! @db-container)
      (reset! db-container nil)))
  testable)

(defn db-setup
  [f]
  (let [mapped-port (-> @db-container :mapped-ports (get 5432))
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
    (mount/start-with {#'db/ds (jdbc/with-options (jdbc/get-datasource db-spec) jdbc/snake-kebab-opts)})
    (f)
    (mount/stop #'db/ds)))

(defn db-cleanup
  [f]
  ;; Continuously check if the database is ready for migration.
  (let [cnt   (atom 10)
        break (atom false)]
    (while (and (not @break) (> @cnt 0))
      (try
        (with-open [conn (jdbc/get-connection db/ds)]
          (if (.isValid conn 1)
            (reset! break true)
            (swap! cnt dec)))
        (catch Exception _ (swap! cnt dec))))
    (if-not @break
      (throw (ex-info "Database connection cannot be established" {}))))
  (migratus/reset @migratus-config)
  (f))
