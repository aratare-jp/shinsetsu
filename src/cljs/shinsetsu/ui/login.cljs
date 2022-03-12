(ns shinsetsu.ui.login
  (:require
    [shinsetsu.mutations :as api]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :refer [div label input form button h1]]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [shinsetsu.ui.elastic :as e]))

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
        username-unchecked? (= :unchecked (login-validator props :ui/username))
        password-unchecked? (= :unchecked (login-validator props :ui/password))
        form-unchecked?     (or username-unchecked? password-unchecked?)
        on-login            (fn [e]
                              (evt/prevent-default! e)
                              (comp/transact! this [(api/login {:user/username username :user/password password})]))
        on-register         (fn [e]
                              (evt/prevent-default! e)
                              (comp/transact! this [(api/register {:user/username username :user/password password})]))
        on-clear            (fn [e]
                              (evt/prevent-default! e)
                              (comp/transact! this [(fs/reset-form! {:form-ident (comp/get-ident this)})]))]
    (e/page-template {:template "centeredBody"}
      (e/empty-prompt {:title (h1 "Login")
                       :body  (e/form {:component "form"}
                                (e/form-row {:label "Username"}
                                  (e/field-text {:name     "username"
                                                 :value    username
                                                 :onChange on-username-changed
                                                 :onBlur   #(on-blur :ui/username)}))
                                (e/form-row {:label "Password"}
                                  (e/field-text {:name     "password"
                                                 :value    password
                                                 :type     "password"
                                                 :onChange on-password-changed
                                                 :onBlur   #(on-blur :ui/password)}))
                                (e/spacer {})
                                (e/flex-group {}
                                  (e/flex-item {}
                                    (e/button {:type     "submit"
                                               :fill     true
                                               :onClick  on-login
                                               :disabled (or form-unchecked? form-invalid?)}
                                      "Login"))
                                  (e/flex-item {}
                                    (e/button {:type     "submit"
                                               :fill     true
                                               :onClick  on-register
                                               :disabled (or form-unchecked? form-invalid?)}
                                      "Register"))
                                  (e/flex-item {}
                                    (e/button {:type    "button"
                                               :fill    true
                                               :onClick on-clear}
                                      "Clear"))))}))))

(def ui-login (comp/factory Login))
