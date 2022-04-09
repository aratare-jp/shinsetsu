(ns user
  (:require
    [shinsetsu.server :refer [repl-server]]
    [clojure.tools.namespace.repl :refer [set-refresh-dirs refresh]]
    [puget.printer :as puget]
    [mount.core :as mount]
    [taoensso.timbre :as log]))

(add-tap (bound-fn* puget/pprint))

;; Ensure we only refresh the source we care about. This is important
;; because `resources` is on our classpath, and we don't want to
;; accidentally pull source from there when cljs builds cache files there.
(set-refresh-dirs "src/dev" "src/clj")

(defn start
  []
  (log/info "Starting server")
  (mount/start-without #'repl-server))

(defn stop
  []
  (log/info "Stopping server")
  (mount/stop-except #'repl-server))

(defn restart
  []
  (stop)
  (log/info "Refreshing all files")
  (refresh :after 'user/start))

(comment
  (start)
  (restart))
