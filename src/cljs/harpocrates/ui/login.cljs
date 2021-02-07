(ns harpocrates.ui.login
  (:require
    [harpocrates.ui.elastic-ui :as cm]
    [harpocrates.mutations.user :refer [login]]
    [harpocrates.application :refer [app]]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :refer [change-route! route-immediate]]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.fulcro.algorithms.merge :as m]
    [com.fulcrologic.fulcro.mutations :as mut]))

(defsc Login
  [this {:keys      [status-code]
         :ui/keys   [is-loading?]
         :user/keys [username password]}]
  {:ident         (fn [] [:component/id :login])
   :query         [:ui/is-loading? :status-code :user/username :user/password]
   :initial-state {:ui/is-loading? false}
   :route-segment ["login"]
   :will-enter    (fn [_ _] (route-immediate [:component/id :login]))}
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
            {:component "form"
             :isInvalid (and status-code (not= status-code 200))
             :error     "Incorrect username or password"}
            (cm/ui-form-row
              {:label     "Username"
               :fullWidth true}
              (cm/ui-field-text
                {:isInvalid (and status-code (not= status-code 200))
                 :name      "username"
                 :value     username
                 :onChange  #(mut/set-string! this :user/username :event %)}))
            (cm/ui-form-row
              {:label     "Password"
               :fullWidth true}
              (cm/ui-field-text
                {:isInvalid (and status-code (not= status-code 200))
                 :name      "password"
                 :value     password
                 :type      "password"
                 :onChange  #(mut/set-string! this :user/password :event %)}))
            (cm/ui-button
              {:isLoading is-loading?
               :disabled  is-loading?
               :onClick   #(comp/transact! this [(login {:user/username username :user/password password})])} "Login")))))))

(def ui-login-form (comp/factory Login))