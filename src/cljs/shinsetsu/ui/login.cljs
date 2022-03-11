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
      :ui/password (not-empty? password)
      false)))

(def login-validator (fs/make-validator login-valid?))

(defsc Login
  [this {:ui/keys [username password] :as props}]
  {:query         [:ui/username :ui/password fs/form-config-join]
   :ident         (fn [] [:component/id ::login])
   :initial-state {:ui/username "" :ui/password ""}
   :route-segment ["login"]
   :form-fields   #{:ui/username :ui/password}
   :pre-merge     (fn [{:keys [data-tree]}] (fs/add-form-config Login data-tree))}
  (let [on-username-changed (fn [e] (m/set-string! this :ui/username :event e))
        on-password-changed (fn [e] (m/set-string! this :ui/password :event e))
        on-blur             (fn [f] (comp/transact! this [(fs/mark-complete! {:field f})]))
        username-invalid?   (= :invalid (login-validator props :ui/username))
        password-invalid?   (= :invalid (login-validator props :ui/password))
        form-invalid?       (or username-invalid? password-invalid?)
        on-login            (fn [e]
                              (evt/prevent-default! e)
                              (comp/transact! this [(api/login {:user/username username :user/password password})]))
        on-register         (fn [e]
                              (evt/prevent-default! e)
                              (comp/transact! this [(api/register {:user/username username :user/password password})]))
        on-clear            (fn [e]
                              (evt/prevent-default! e)
                              (comp/transact! this [(fs/reset-form! {:form-ident (comp/get-ident this)})]))]
    (div :.row
         (div :.col
              (form
                (div :.form-floating.mb-3
                     (input :#username.form-control.form-control-lg
                            {:classes     [(if username-invalid? "is-invalid")]
                             :value       username
                             :onChange    on-username-changed
                             :onBlur      #(on-blur :ui/username)
                             :placeholder "Username"})
                     (label :.form-label {:htmlFor "username"} "Username"))
                (div :.form-floating.mb-3
                     (input :#password.form-control.form-control-lg
                            {:classes     [(if password-invalid? "is-invalid")]
                             :type        "password"
                             :value       password
                             :onChange    on-password-changed
                             :onBlur      #(on-blur :ui/password)
                             :placeholder "Password"})
                     (label :.form-label {:htmlFor "password"} "Password"))
                (div :.btn-group {:role "group"}
                     (button :.btn.btn-primary.btn-lg
                             {:type     "submit"
                              :onClick  on-login
                              :disabled form-invalid?}
                             "Login")
                     (button :.btn.btn-success.btn-lg
                             {:type     "submit"
                              :onClick  on-register
                              :disabled form-invalid?}
                             "Register")
                     (button :.btn.btn-secondary.btn-lg
                             {:type    "button"
                              :onClick on-clear}
                             "Clear")))))))

(def ui-login (comp/factory Login))
