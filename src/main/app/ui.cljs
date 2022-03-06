(ns app.ui
  (:require
    [app.mutations :as api]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.fulcro.dom.events :as evt]))

(defsc Person [this {:person/keys [id name age] :as props} {:keys [onDelete]}]
  {:query [:person/id :person/name :person/age]
   :ident (fn [] [:person/id (:person/id props)])}
  (dom/li
    (dom/h5 (str name " (age: " age ")") (dom/button {:onClick #(onDelete id)} "X"))))

(def ui-person (comp/factory Person {:keyfn :person/id}))

(defsc PersonList [this {:list/keys [id label people] :as props}]
  {:query [:list/id :list/label {:list/people (comp/get-query Person)}]
   :ident (fn [] [:list/id (:list/id props)])}
  (let [delete-person (fn [person-id] (comp/transact! this [(api/delete-person {:list/id id :person/id person-id})]))]
    (dom/div
      (dom/h4 label)
      (dom/ul
        (map #(ui-person (comp/computed % {:onDelete delete-person})) people)))))

(def ui-person-list (comp/factory PersonList))



(defsc LoginForm [this {:ui/keys [username password] :as props}]
  {:query         [:ui/username :ui/password]
   :ident         (fn [] [:component :login])
   :initial-state {:ui/username "hello" :ui/password "world"}}
  (let [on-username-changed (fn [e] (m/set-string! this :ui/username :event e))
        on-password-changed (fn [e] (m/set-string! this :ui/password :event e))
        on-submit           (fn [e]
                              (evt/prevent-default! e)
                              ;; Send mutations here
                              (comp/transact! this [(api/login {:username username :password password})]))
        on-cancel           (fn [e]
                              (evt/prevent-default! e)
                              (reset! username "")
                              (reset! password ""))]
    (dom/div :.row
             (dom/div :.col
                      (dom/form
                        (dom/div :.mb-3
                                 (dom/label :.form-label {:htmlFor "username"} "Username")
                                 (dom/input :#username.form-control {:value username :onChange on-username-changed}))
                        (dom/div :.mb-3
                                 (dom/label :.form-label {:htmlFor "password"} "Password")
                                 (dom/input :#password.form-control {:type "password" :value password :onChange on-password-changed}))
                        (dom/button :.btn.btn-primary {:type "submit" :onClick on-submit} "Login")
                        (dom/button :.btn.btn-secondary {:onClick on-cancel} "Clear"))))))

(def ui-login-form (comp/factory LoginForm))

(defsc Root [this {:keys [login-data] :as props}]
  {:query         [{:login-data (comp/get-query LoginForm)}]
   :initial-state (fn [p] {:login-data (comp/get-initial-state LoginForm)})}
  (ui-login-form login-data))
