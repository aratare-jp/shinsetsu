(ns shinsetsu.mutations
  (:require
    [shinsetsu.application :refer [app]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]))

(def login-token (atom nil))

(defmutation login
  "Login with a username and password"
  [{:user/keys [username password]}]
  (remote [env] true)
  (ok-action
    [{:keys [result] :as env}]
    (swap! login-token #(-> result (get :body) vals first))
    #_(dr/change-route app ["main"]))
  (error-action
    [env]
    (js/alert "Oops seems like your credentials are not correct.")))

(defmutation register
  "Register with a username and password"
  [{:user/keys [username password]}]
  (remote [_] true)
  (ok-action
    [{:keys [result] :as env}]
    (swap! login-token #(-> result (get :body) vals first))
    #_(dr/change-route app ["main"]))
  (error-action
    [env]
    (js/alert "Oops seems like your credentials are not correct.")))

(defmutation clear-form
  "Clear a form back to its pristine stage"
  [{:keys [ident]}]
  (action
    [{:keys [state]}]
    (swap! state fs/pristine->entity* ident)))
