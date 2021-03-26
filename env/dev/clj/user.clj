(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require
    [harpocrates.config :refer [env]]
    [clojure.pprint :refer [pprint]]
    [mount.core :as mount]
    [harpocrates.core :refer [repl-server]]
    [harpocrates.db.core :refer [db]]
    [harpocrates.config :refer [env]]
    [clojure.tools.namespace.repl :refer [refresh]]
    [migratus.core :as migratus]))

(add-tap (bound-fn* pprint))

(mount/start #'env #'db)

(def migratus-config {:store         :database
                      :migration-dir "migrations/"
                      :db            {:datasource db}})

(defn start
  "Starts application."
  []
  (mount/start-without #'repl-server))

(defn stop
  "Stops application."
  []
  (mount/stop-except #'repl-server))

(defn restart
  "Restarts application."
  []
  (stop)
  (refresh :after `start))

(defn restart-db
  "Restarts database."
  []
  (mount/stop #'db)
  (mount/start #'db))

(defn reset-db
  "Resets database."
  []
  (migratus/reset migratus-config))

(defn migrate
  "Migrates database up for all outstanding migrations."
  []
  (migratus/migrate migratus-config))

(defn rollback
  "Rollback latest database migration."
  []
  (migratus/rollback migratus-config))

(defn up
  "Bring up migrations matching the given ids"
  [& ids]
  (apply migratus/up migratus-config ids))

(defn down
  "Bring down migrations matching the given ids"
  [& ids]
  (apply migratus/down migratus-config ids))

(defn create-migration
  "Create a new up and down migration file with a generated timestamp and `name`."
  [name]
  (migratus/create migratus-config name))

(comment
  (reset-db)
  (rollback)
  (migrate)
  (create-migration "create-user")
  (create-migration "create-bookmark")
  (create-migration "create-tab")
  (create-migration "create-tag")
  (create-migration "create-bookmark-tag")
  (create-migration "create-tab-tag"))