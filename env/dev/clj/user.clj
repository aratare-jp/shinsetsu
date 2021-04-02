(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require
    [shinsetsu.config :refer [env]]
    [clojure.pprint :refer [pprint]]
    [mount.core :as mount]
    [shinsetsu.core :refer [repl-server]]
    [shinsetsu.db.core :refer :all]
    [shinsetsu.config :refer [env]]
    [clojure.tools.namespace.repl :refer [refresh]]
    [schema.core :as s]))

(add-tap (bound-fn* pprint))

(mount/start #'env #'db #'migratus-config)

(s/set-fn-validation! true)

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
