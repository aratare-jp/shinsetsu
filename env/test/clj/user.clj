(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require
    [shinsetsu.config :refer [env]]
    [clojure.pprint :refer [pprint]]
    [mount.core :as mount]
    [shinsetsu.core :refer [repl-server]]
    [shinsetsu.db.core :refer [db]]
    [clojure.tools.namespace.repl :refer [refresh]]
    [schema.core :as s]))

(add-tap (bound-fn* pprint))

(s/set-fn-validation! true)
