;;(ns harpocrates.figwheel
;;  (:require [figwheel-sidecar.repl-api :as ra]))

(ns harpocrates.figwheel)


(defn start-fw []
  (ra/start-figwheel!))

(defn stop-fw []
  (ra/stop-figwheel!))

(defn cljs []
  (ra/cljs-repl))

