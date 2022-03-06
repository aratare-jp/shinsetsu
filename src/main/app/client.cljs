(ns app.client
  (:require
    [app.application :refer [app]]
    [app.ui :as ui]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.data-fetch :as df]))

(defn ^:export init []
  (app/mount! app ui/Root "app")
  (dr/initialize! app)
  (dr/change-route app ["login"])
  (js/console.log "Loaded"))

(defn ^:export refresh []
  (app/mount! app ui/Root "app")
  (comp/refresh-dynamic-queries! app)
  (js/console.log "Hot reload"))
