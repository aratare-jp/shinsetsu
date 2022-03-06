(ns app.client
  (:require
    [app.application :refer [app]]
    [app.ui :as ui]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.data-fetch :as df]))

(defn ^:export init []
  (merge/merge-component! app ui/LoginForm (comp/get-initial-state ui/LoginForm))
  (app/mount! app ui/Root "app")
  #_(df/load! app :friends ui/PersonList)
  (js/console.log "Loaded"))

(defn ^:export refresh []
  (merge/merge-component! app ui/LoginForm (comp/get-initial-state ui/LoginForm))
  ;; re-mounting will cause forced UI refresh
  (app/mount! app ui/Root "app")
  ;; 3.3.0+ Make sure dynamic queries are refreshed
  (comp/refresh-dynamic-queries! app)
  (js/console.log "Hot reload"))
