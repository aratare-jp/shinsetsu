(ns shinsetsu.ui.user
  (:require [com.fulcrologic.fulcro.components :refer-macros [defsc] :as comp]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
            [shinsetsu.ui.elastic-ui :refer [ui-button]]
            [shinsetsu.ui.tab :refer [Tab]]
            [shinsetsu.mutations.user :refer [logout]]))

(defsc CurrentUser
  [this {:user/keys [valid?]}]
  {:query         [:user/id :user/email :user/valid?
                   {:user/tabs (comp/get-query Tab)}]
   :initial-state {:user/id     :nobody
                   :user/valid? false
                   :user/tabs   []}}
  (dom/div
    (if valid?
      (ui-button {:onClick #(comp/transact! this [(logout)])} "Logout")
      (ui-button {:onClick #(dr/change-route! this "login")} "Login"))))

(def ui-current-user (comp/factory CurrentUser))

(defsc User
  [this {:user/keys [id email valid? tabs] :as props}]
  {:ident         :user/id
   :query         [:user/id :user/email :user/valid?
                   {:user/tabs (comp/get-query Tab)}]
   :initial-state {:user/id     :nobody
                   :user/valid? false
                   :user/tabs   []}}
  (dom/div "User"))

(def ui-user (comp/factory User))
