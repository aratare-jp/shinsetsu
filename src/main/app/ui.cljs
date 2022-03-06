(ns app.ui
  (:require
    [app.mutations :as api]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom :refer [div label input form button]]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.fulcro.dom.events :as evt]))

#_(defsc Person [this {:person/keys [id name age] :as props} {:keys [onDelete]}]
    {:query [:person/id :person/name :person/age]
     :ident (fn [] [:person/id (:person/id props)])}
    (li
      (h5 (str name " (age: " age ")") (button {:onClick #(onDelete id)} "X"))))

#_(def ui-person (comp/factory Person {:keyfn :person/id}))

#_(defsc PersonList [this {:list/keys [id label people] :as props}]
    {:query [:list/id :list/label {:list/people (comp/get-query Person)}]
     :ident (fn [] [:list/id (:list/id props)])}
    (let [delete-person (fn [person-id] (comp/transact! this [(api/delete-person {:list/id id :person/id person-id})]))]
      (div
        (h4 label)
        (ul
          (map #(ui-person (comp/computed % {:onDelete delete-person})) people)))))

#_(def ui-person-list (comp/factory PersonList))

(defsc LoginForm [this {:ui/keys [username password] :as props}]
  {:query         [:ui/username :ui/password]
   :ident         (fn [] [:component :login])
   :initial-state {:ui/username "" :ui/password ""}}
  (let [on-username-changed (fn [e] (m/set-string! this :ui/username :event e))
        on-password-changed (fn [e] (m/set-string! this :ui/password :event e))
        on-submit           (fn [e]
                              (evt/prevent-default! e)
                              ;; Send mutations here
                              (comp/transact! this [(api/login {:username username :password password})]))
        on-cancel           (fn [e]
                              (evt/prevent-default! e)
                              (m/set-value! this :ui/username "")
                              (m/set-value! this :ui/password ""))]
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

(def ui-login-form (comp/factory LoginForm))

(defsc Root [this {:keys [login-data] :as props}]
  {:query         [{:login-data (comp/get-query LoginForm)}]
   :initial-state (fn [p] {:login-data (comp/get-initial-state LoginForm)})}
  (ui-login-form login-data))
