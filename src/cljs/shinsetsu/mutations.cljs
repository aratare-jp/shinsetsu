(ns shinsetsu.mutations
  (:require
    [shinsetsu.application :refer [app login-token]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]))

(defmutation login
  "Login with a username and password"
  [_]
  (auth [_] true)
  (ok-action
    [{:keys [result]}]
    (let [token (-> result (get-in [:body `login :token]))]
      (if (not= :invalid token)
        (do
          (reset! login-token token)
          (dr/change-route app ["main"]))
        (js/alert "Oops seems like your credentials are incorrect."))))
  (error-action
    [_]
    (js/alert "Oops seems like we're down at the moment.")))

(defmutation register
  "Register with a username and password"
  [_]
  (auth [_] true)
  (ok-action
    [{:keys [result]}]
    (let [token (-> result (get-in [:body `register :token]))]
      (if (not= :invalid token)
        (do
          (reset! login-token token)
          (dr/change-route app ["main"]))
        (js/alert "Oops seems like this username already exists."))))
  (error-action
    [_]
    (js/alert "Oops seems like your credentials are not correct.")))

(defmutation clear-form
  "Clear a form back to its pristine stage"
  [{:keys [ident]}]
  (action
    [{:keys [state]}]
    (swap! state fs/pristine->entity* ident)))

(defmutation create-tab
  "Create a new tab"
  [_]
  (protected [_] true)
  (ok-action
    [{:keys [result]}]
    (js/console.log result)))
