(ns harpocrates.mutations.user
  (:require [com.fulcrologic.fulcro.mutations :refer-macros [defmutation] :as m]
            [com.fulcrologic.fulcro.components :refer [registry-key->class]]
            [com.fulcrologic.fulcro.algorithms.merge :refer [merge-component!]]
            [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]
            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]))

(defmutation login
  "Login with the given username and password."
  [_]
  (action
    [{:keys [state]}]
    (swap! state assoc-in [:component/id :login :ui/is-loading?] true))
  (remote
    [env]
    true)
  (ok-action
    [{:keys [state app result] :as env}]
    (js/console.log result)
    (swap! state assoc-in [:root/current-user] (-> result :body (get `login) :user/token))
    (dr/change-route! app ["main"]))
  (error-action
    [{:keys [state]}]
    (js/console.error "ERROR WHEN LOGGING IN!")))
