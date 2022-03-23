(ns shinsetsu.ui.login
  (:require
    [shinsetsu.mutations.user :as um]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :refer [div label input form button h1 h2]]
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
  [this {:ui/keys [username password loading? error-type] :as props}]
  {:query         [:ui/username :ui/password :ui/loading? :ui/error-type fs/form-config-join]
   :ident         (fn [] [:component/id ::login])
   :initial-state {:ui/username "" :ui/password "" :ui/loading? false}
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
                              (m/set-value! this :ui/loading? true)
                              (comp/transact! this [(um/login {:user/username username :user/password password})]))
        on-register         (fn [e]
                              (evt/prevent-default! e)
                              (m/set-value! this :ui/loading? true)
                              (comp/transact! this [(um/register {:user/username username :user/password password})]))
        on-clear            (fn [e]
                              (evt/prevent-default! e)
                              (comp/transact! this [(fs/reset-form! {:form-ident (comp/get-ident this)})]))
        errors              (case error-type
                              :duplicate-user ["This username has already been used."
                                               "Please try again with different username."]
                              :wrong-credentials ["It seems you're trying to login with the wrong username or password."
                                                  "Please try again."]
                              :internal-server-error ["Unknown error encountered."
                                                      "It is highly likely an internal server error."
                                                      "Please try again later."]
                              nil)]
    (e/page-template {:template "centeredBody"}
      (if loading?
        (e/empty-prompt {:icon  (e/loading-spinner {:size "xxl"})
                         :title (h2 {} "Processing your request...")})
        (e/empty-prompt {:title (h1 "Login")
                         :body  (e/form {:component "form" :isInvalid (boolean errors) :error errors}
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
                                                 :disabled form-invalid?}
                                        "Login"))
                                    (e/flex-item {}
                                      (e/button {:type     "submit"
                                                 :fill     true
                                                 :onClick  on-register
                                                 :disabled form-invalid?}
                                        "Register"))
                                    (e/flex-item {}
                                      (e/button {:type    "button"
                                                 :onClick on-clear}
                                        "Clear"))))})))))

(def ui-login (comp/factory Login))
