(ns shinsetsu.mutations.user
  (:require
    [shinsetsu.application :refer [app login-token]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.components :as comp]
    [taoensso.timbre :as log]))

(defmutation login
  "Login with a username and password"
  [_]
  (auth [_] true)
  (ok-action
    [{:keys [result]}]
    (let [token (-> result (get-in [:body `login :token]))]
      (reset! login-token token)
      (dr/change-route app ["main"])))
  (error-action
    [{:keys [state ref] {body :body} :result}]
    (let [{:keys [error-type]} (get body `login)]
      (swap! state assoc-in (conj ref :ui/loading?) false)
      (swap! state assoc-in (conj ref :ui/error-type) error-type))))

(defmutation register
  "Register with a username and password"
  [_]
  (auth [_] true)
  (ok-action
    [{:keys [result]}]
    (let [token (-> result (get-in [:body `register :token]))]
      (reset! login-token token)
      (dr/change-route app ["main"])))
  (error-action
    [{:keys [state ref] {body :body} :result}]
    (let [{:keys [error-type]} (get body `register)]
      (swap! state assoc-in (conj ref :ui/loading?) false)
      (swap! state assoc-in (conj ref :ui/error-type) error-type))))
