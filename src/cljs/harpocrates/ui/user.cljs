(ns harpocrates.ui.user
  (:require [com.fulcrologic.fulcro.components :refer-macros [defsc] :as comp]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
            [harpocrates.ui.elastic-ui :refer [ui-button]]
            [harpocrates.mutations.user :refer [logout]]))

(defsc CurrentUser
  [this {:keys [:user/id :user/email :user/valid?] :as props}]
  {:query         [:user/id :user/email :user/valid?]
   :initial-state {:user/id     :nobody
                   :user/valid? false}}
  (dom/div
    (if valid?
      (ui-button {:onClick #(comp/transact! this [(logout)])} "Logout")
      (ui-button {:onClick #(dr/change-route! this "login")} "Login"))))

(def ui-current-user (comp/factory CurrentUser))