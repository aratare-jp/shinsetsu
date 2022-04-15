(ns shinsetsu.mutations.user
  (:require
    [medley.core :refer [dissoc-in]]
    [shinsetsu.application :refer [app]]
    [shinsetsu.store :refer [store get-key set-key]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.components :as comp]
    [taoensso.timbre :as log]))

(defn- move-to-tab
  []
  (let [TabMain (comp/registry-key->class `shinsetsu.ui.tab/TabMain)
        rs      (dr/route-segment TabMain)]
    (dr/change-route app rs)))

(defn- move-to-login
  []
  (let [Login (comp/registry-key->class `shinsetsu.ui.login/Login)
        rs    (dr/route-segment Login)]
    (dr/change-route app rs)))

(defmutation login
  [_]
  (action
    [{:keys [state ref]}]
    (log/debug "Logging user in")
    (swap! state assoc-in (conj ref :ui/loading?) true))
  (auth [_] true)
  (ok-action
    [{{{{:user/keys [token]} `login} :body} :result :keys [ref state]}]
    (log/debug "User logged in successfully")
    (swap! store set-key :userToken token)
    (swap! state dissoc-in (conj ref :ui/password))
    (move-to-tab))
  (error-action
    [{{{{:keys [error-type error-message]} `login} :body} :result :keys [state ref]}]
    (log/debug "User failed to login due to:" error-message)
    (swap! state #(-> %
                      (assoc-in (conj ref :ui/loading?) false)
                      (assoc-in (conj ref :ui/error-type) error-type)))))

(defmutation register
  [_]
  (action
    [{:keys [state ref]}]
    (log/debug "Registering user")
    (swap! state assoc-in (conj ref :ui/loading?) true))
  (auth [_] true)
  (ok-action
    [{{{{:user/keys [token]} `register} :body} :result :keys [ref state]}]
    (log/debug "User registered successfully")
    (swap! store set-key :userToken token)
    (swap! state dissoc-in (conj ref :ui/password))
    (move-to-tab))
  (error-action
    [{{{{:keys [error-type error-message]} `register} :body} :result :keys [state ref]}]
    (log/debug "User failed to register due to:" error-message)
    (swap! state #(-> %
                      (assoc-in (conj ref :ui/loading?) false)
                      (assoc-in (conj ref :ui/error-type) error-type)))))

(defmutation fetch-current-user
  [_]
  (action
    [_]
    (if (get-key @store :userToken)
      (move-to-tab)
      (move-to-login))))
