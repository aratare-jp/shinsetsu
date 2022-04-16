(ns shinsetsu.client
  (:require
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [shinsetsu.application :refer [app]]
    [shinsetsu.mutations.user :refer [fetch-current-user]]
    [shinsetsu.ui.root :refer [Root]]
    [taoensso.timbre :as log]))

(defn ^:export init
  []
  (enable-console-print!)
  (app/set-root! app Root {:initialize-state? true})
  (dr/initialize! app)
  (comp/transact! app [(fetch-current-user nil)])
  (app/mount! app Root "app")
  (log/info "Loaded"))

(defn ^:export refresh
  []
  (app/mount! app Root "app")
  (comp/refresh-dynamic-queries! app)
  (log/info "Hot reloaded"))
