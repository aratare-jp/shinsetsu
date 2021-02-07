(ns harpocrates.ui.login
  (:require
    [harpocrates.ui.elastic-ui :as cm]
    [harpocrates.mutations.user :refer [login]]
    [harpocrates.application :refer [app]]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :refer [change-route! route-immediate]]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.fulcro.algorithms.merge :as m]))

(defsc Login
  [this {:keys [ui/is-loading?]}]
  {:ident         (fn [] [:component/id :login])
   :query         [:ui/is-loading?]
   :initial-state {:ui/is-loading? false}
   :route-segment ["login"]
   :will-enter    (fn [_ _] (route-immediate [:component/id :login]))}
  (js/console.log (str "Token: " user))
  (let [current-user (atom {})]
    (cm/ui-page
      {:className "full-height"}
      (cm/ui-page-body
        {:component "div"}
        (cm/ui-page-content
          {:verticalPosition   "center"
           :horizontalPosition "center"
           :paddingSize        "s"
           :panelPaddingSize   "s"}
          (cm/ui-page-content-header
            nil
            (cm/ui-page-content-header-section
              nil
              (cm/ui-title
                nil
                (dom/h1 "Login"))))
          (cm/ui-page-content-body
            nil
            (cm/ui-form
              {:component "form"}
              (cm/ui-form-row
                {:label     "Username"
                 :fullWidth true}
                (cm/ui-field-text
                  {:name     "username"
                   :onChange #(swap! current-user assoc :user/username (evt/target-value %))}))
              (cm/ui-form-row
                {:label     "Password"
                 :fullWidth true}
                (cm/ui-field-text
                  {:name     "password"
                   :type     "password"
                   :onChange #(swap! current-user assoc :user/password (evt/target-value %))}))
              (cm/ui-button
                {:isLoading is-loading?
                 :disabled  is-loading?
                 :onClick   #(comp/transact! this [(login @current-user)])} "Login"))))))))

(def ui-login-form (comp/factory Login))