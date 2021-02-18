(ns harpocrates.mutations.user
  (:require
    [com.fulcrologic.fulcro.algorithms.merge :refer [merge-component!]]
    [com.fulcrologic.fulcro.components :refer [registry-key->class]]
    [com.fulcrologic.fulcro.mutations :refer-macros [defmutation] :as m]
    [com.fulcrologic.fulcro.algorithms.denormalize :as fdn]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [medley.core :refer [dissoc-in]]
    [taoensso.timbre :as log]
    [harpocrates.db :refer [db]]
    [harpocrates.routing :as routing]))

(defn show-login-busy* [state-map tf]
  (assoc-in state-map [:component/id :login :ui/busy?] tf))

(defn show-login-error* [state-map tf]
  (assoc-in state-map [:component/id :login :ui/error?] tf))

(defn clear-credentials [state-map]
  (-> state-map
      (assoc-in [:component/id :login :ui/email] "")
      (assoc-in [:component/id :login :ui/password] "")))

(defmutation login [_]
  (action [{:keys [state]}] (swap! state show-login-busy* true))
  (error-action [{:keys [state]}]
                (log/error "Error action")
                (swap! state (fn [s] (-> s (show-login-busy* false) (show-login-error* true)))))
  (ok-action [{:keys [state]}]
             (log/info "OK action")
             (let [current-user-query [{:session/current-user [:user/valid?]}]
                   {{:user/keys [valid?]} :session/current-user} (fdn/db->tree current-user-query @state @state)]
               (if [valid?]
                 (do
                   (swap! state (fn [s] (-> s (show-login-busy* false) (show-login-error* false) clear-credentials)))
                   (routing/route-to! "/main"))
                 (swap! state (fn [s]
                                (-> s
                                    (show-login-busy* false)
                                    (show-login-error* true)))))))
  (refresh [_] [:ui/error? :ui/busy?])
  (remote [env] (-> env (m/returning `harpocrates.ui.user/User) (m/with-target [:session/current-user]))))

(defmutation logout [_]
  (action [{:keys [state]}]
          (routing/route-to! "/login")
          ;; TODO: Need to clear everything when logging out, not just the user.
          (swap! state assoc :session/current-user {:user/id :nobody :user/valid? false}))
  (remote [env] true))

(defmutation finish-login [_]
  (action [{:keys [state]}]
          (let [current-user-query [{:session/current-user [:user/valid?]}]
                {{:user/keys [valid?]} :session/current-user} (fdn/db->tree current-user-query @state @state)]
            (if valid?
              (routing/route-to! "/main")
              (routing/route-to! "/login"))
            (swap! state assoc :root/ready? true))))