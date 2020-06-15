(ns harpocrates.ui
  (:require
    [harpocrates.mutations :as api]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :refer [h1 div form li h5 ul button h3 label input]]))

(defsc Person [this {:person/keys [id name age] :as props} {:keys [onDelete]}]
  {:query [:person/id :person/name :person/age]
   :ident (fn [] [:person/id (:person/id props)])}
  (li
    (h5 (str name " (age: " age ")") (button {:onClick #(onDelete id)} "Delete")))) ; (4)

(def ui-person (comp/factory Person {:keyfn :person/id}))

(defsc PersonList [this {:list/keys [id label people] :as props}]
  {:query [:list/id :list/label {:list/people (comp/get-query Person)}]
   :ident (fn [] [:list/id (:list/id props)])}
  (let [delete-person (fn [person-id] (comp/transact! this [(api/delete-person {:list/id id :person/id person-id})]))] ; (2)
    (div
      (ul
        (map #(ui-person (comp/computed % {:onDelete delete-person})) people)))))

(def ui-person-list (comp/factory PersonList))


;; ------------------------------
;; Login

(defsc Login [this _]
  (div
    (form
      (h1 "Login")
      (div :.form-group
           (label {:for "email-address"} "Email Address")
           (input :#email-address.form-control {:type "email"}))
      (div :.form-group
           (label {:for "password"} "Password")
           (input :#password.form-control {:type "password"}))
      (div :.btn-group {:role "group" :aria-label "Button Group"}
           (button :.btn.btn-primary {:type "button"} "Login")))))

(def ui-login (comp/factory Login))

(defsc Root [this {:keys [friends enemies]}]
  {:query         [{:friends (comp/get-query PersonList)}
                   {:enemies (comp/get-query PersonList)}]
   :initial-state {}}
  (ui-login {}))
