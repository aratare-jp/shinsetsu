(ns shinsetsu.core
  (:require
    [com.fulcrologic.fulcro.algorithms.timbre-support :refer [console-appender prefix-output-fn]]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]
    [taoensso.timbre :as log]
    [shinsetsu.application :refer [app]]
    [shinsetsu.routing :as routing]
    [shinsetsu.ui.main :refer [Main]]
    [shinsetsu.ui.root :refer [Root]]
    [shinsetsu.ui.user :refer [CurrentUser User]]
    [shinsetsu.mutations.user :refer [finish-login]]))

(defn ^:export refresh []
  ;; re-mounting will cause forced UI refresh
  (app/mount! app Root "app")
  ;; 3.3.0+ Make sure dynamic queries are refreshed
  (comp/refresh-dynamic-queries! app)
  (js/console.log "Hot reload"))

(defn ^:export init []
  (log/merge-config! {:output-fn prefix-output-fn
                      :appenders {:console (console-appender)}})
  (log/info "Starting App")
  (app/mount! app Root "app")
  (dr/initialize! app)
  (routing/start!)
  (df/load! app :session/current-user User {:post-mutation `finish-login}))
