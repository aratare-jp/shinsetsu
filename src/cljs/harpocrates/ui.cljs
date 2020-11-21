(ns harpocrates.ui
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom]))

;; ------------------------------
;; Login

(defsc Login [_ _]
  (dom/div
    (dom/form
      (dom/h1 "Login")
      (dom/div :.form-group
               (dom/label {:for "email-address"} "Email Address")
               (dom/input :#email-address.form-control {:type "email"}))
      (dom/div :.form-group
               (dom/label {:for "password"} "Password")
               (dom/input :#password.form-control {:type "password"}))
      (dom/div :.btn-group {:role "group" :aria-label "Button Group"}
               (dom/button :.btn.btn-primary {:type "button"} "Login")))))

(def ui-login (comp/factory Login))

(defsc Root [_ _]
  {:initial-state {}}
  (ui-login {}))
