(ns harpocrates.mutations.user
  (:require [com.fulcrologic.fulcro.mutations :refer-macros [defmutation] :as m]
            [com.fulcrologic.fulcro.components :refer [registry-key->class]]
            [com.fulcrologic.fulcro.algorithms.merge :refer [merge-component!]]
            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]))

(defmutation login
  "Login with the given username and password."
  [_]
  (action [{:keys [state]}]
          (swap! state assoc-in [:component/id :login :ui/is-loading?] true))
  (remote [_] true)
  (ok-action [{:keys [state app result]}]
             (js/console.log "Logged in!")
             (swap! state assoc-in [:root/current-user] (-> result :body (get `login) :user/token))
             (dr/change-route! app ["main"]))
  (error-action [{{:keys [body]} :result :keys [state app component] :as env}]
                (js/console.log "Error when logged in!")
                (merge-component! app component (get body `login))
                (swap! state assoc-in [:component/id :login :ui/is-loading?] false)))
