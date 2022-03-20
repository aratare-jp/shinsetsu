(ns user
  (:require
    [shinsetsu.server :as server]
    [clojure.tools.namespace.repl :refer [set-refresh-dirs refresh]]
    [puget.printer :as puget]))

(add-tap (bound-fn* puget/pprint))

;; Ensure we only refresh the source we care about. This is important
;; because `resources` is on our classpath, and we don't want to
;; accidentally pull source from there when cljs builds cache files there.
(set-refresh-dirs "src/dev" "src/clj")

(defn start
  []
  (server/start-app))

(defn restart
  []
  (server/stop-app)
  (refresh :after 'user/start))
