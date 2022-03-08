(ns shinsetsu.ui.login
  (:require
    [shinsetsu.mutations :as api]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :refer [div label input form button]]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]))

(defn login-valid?
  [{:ui/keys [username password]} field]
  (let [not-empty? (complement empty?)]
    (case field
      :ui/username (not-empty? username)
      :user/password (not-empty? password)
      false)))

(def login-validator (fs/make-validator login-valid?))

(defsc Login
  [this {:ui/keys [username password] :as props}]
  {:query         [:ui/username :ui/password fs/form-config-join]
   :ident         (fn [] [:component/id :login])
   :initial-state {:ui/username "" :ui/password ""}
   :route-segment ["login"]
   :form-fields   #{:ui/username :ui/password}
   :pre-merge     (fn [{:keys [data-tree]}] (fs/add-form-config Login data-tree))}
  (js/console.log (login-validator props :ui/username))
  (let [on-username-changed (fn [e] (m/set-string! this :ui/username :event e))
        on-password-changed (fn [e] (m/set-string! this :ui/password :event e))
        on-submit           (fn [e]
                              (evt/prevent-default! e)
                              (fs/mark-complete! {:field :ui/username})
                              (fs/mark-complete! {:field :ui/password})
                              (if (and (= :valid (login-validator props :ui/username))
                                       (= :valid (login-validator props :ui/password)))
                                ((comp/transact! this [(api/login {:username username :password password})]))))
        on-cancel           (fn [e]
                              (evt/prevent-default! e)
                              (comp/transact! this [(fs/reset-form! {:form-ident (comp/get-ident this)})]))]
    (div :.row
         (div :.col
              (form
                (div :.form-floating.mb-3
                     (input :#username.form-control.form-control-lg {:value       username
                                                                     :onChange    on-username-changed
                                                                     :placeholder "Username"})
                     (label :.form-label {:htmlFor "username"} "Username"))
                (div :.form-floating.mb-3
                     (input :#password.form-control.form-control-lg {:type        "password"
                                                                     :value       password
                                                                     :onChange    on-password-changed
                                                                     :placeholder "Password"})
                     (label :.form-label {:htmlFor "password"} "Password"))
                (div :.btn-group {:role "group"}
                     (button :.btn.btn-primary.btn-lg {:type "submit" :onClick on-submit} "Login")
                     (button :.btn.btn-secondary.btn-lg {:type "button" :onClick on-cancel} "Clear")))))))

(def ui-login (comp/factory Login))
