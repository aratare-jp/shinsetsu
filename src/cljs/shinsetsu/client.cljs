(ns shinsetsu.client
  (:require
    [shinsetsu.application :refer [app]]
    [shinsetsu.ui.root :refer [Root]]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]))

(defn ^:export init []
  (app/mount! app Root "app")
  (dr/initialize! app)
  (dr/change-route app ["login"])
  (js/console.log "Loaded"))

(defn ^:export refresh []
  (app/mount! app Root "app")
  (comp/refresh-dynamic-queries! app)
  (js/console.log "Hot reload"))
