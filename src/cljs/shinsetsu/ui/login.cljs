(ns shinsetsu.ui.login
  (:require
    [shinsetsu.ui.elastic-ui :as cm]
    [shinsetsu.mutations.user :refer [login]]
    [shinsetsu.application :refer [app]]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.components :as comp :refer-macros [defsc]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :refer [change-route! route-immediate]]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.fulcro.mutations :as m]))

(defsc Login
  [this {:ui/keys [email password error? busy?] :as props}]
  {:query         [:ui/email :ui/password :ui/error? :ui/busy?]
   :ident         (fn [] [:component/id :login])
   :route-segment ["login"]
   :initial-state {:ui/email    ""
                   :ui/password ""
                   :ui/error?   false
                   :ui/busy?    false}}
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
             :error     "Incorrect username or password"
             :isInvalid error?}
            (cm/ui-form-row
              {:label     "Username"
               :fullWidth true}
              (cm/ui-field-text
                {:value     email
                 :disabled  busy?
                 :onChange  #(m/set-string! this :ui/email :event %)
                 :isInvalid error?}))
            (cm/ui-form-row
              {:label     "Password"
               :fullWidth true}
              (cm/ui-field-text
                {:type      "password"
                 :value     password
                 :disabled  busy?
                 :onKeyDown (fn [evt]
                              (when (evt/enter-key? evt)
                                (comp/transact! this [(login {:user/email email :user/password password})])))
                 :onChange  #(m/set-string! this :ui/password :event %)
                 :isInvalid error?}))
            (cm/ui-button
              {:isLoading busy?
               :disabled  busy?
               :onClick   #(comp/transact! this [(login {:user/email email :user/password password})])}
              "Login")))))))

(def ui-login-form (comp/factory Login))