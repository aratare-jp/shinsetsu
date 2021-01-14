(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require
    [harpocrates.config :refer [env]]
    [clojure.pprint :refer [pprint]]
    [mount.core :as mount]
    [harpocrates.core :refer [repl-server]]
    [harpocrates.db.core :refer [*db*]]
    [clojure.tools.namespace.repl :refer [refresh]]))

(add-tap (bound-fn* pprint))

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
  (mount/stop #'*db*)
  (mount/start #'*db*))

;(defn reset-db
;  "Resets database."
;  []
;  (migrations/migrate ["reset"] (select-keys env [:database-url])))
;
;(defn migrate
;  "Migrates database up for all outstanding migrations."
;  []
;  (migrations/migrate ["migrate"] (select-keys env [:database-url])))
;
;(defn rollback
;  "Rollback latest database migration."
;  []
;  (migrations/migrate ["rollback"] (select-keys env [:database-url])))
;
;(defn create-migration
;  "Create a new up and down migration file with a generated timestamp and `name`."
;  [name]
;  (migrations/create name (select-keys env [:database-url])))