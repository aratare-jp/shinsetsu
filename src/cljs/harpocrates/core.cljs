(ns harpocrates.core
  (:require
    [cljs.pprint :refer [cl-format]]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.algorithms.react-interop :as interop]
    [tick.alpha.api :as tick]
    [cljsjs.moment]
    ["@elastic/eui" :refer [EuiButton EuiFormRow EuiDatePicker]]))

(defonce app (app/fulcro-app))
(def dark-mode? (atom false))
(def person (atom {:person/name "Joe" :person/age 22}))
(def startDate (js/moment))

(defsc Person [this {:person/keys [name age]}]
  (dom/div
    (dom/p "Name: " name)
    (dom/p "Age: " age)))

(def ui-person (comp/factory Person))
(def ui-button (interop/react-factory EuiButton))
(def ui-form-row (interop/react-factory EuiFormRow))
(def ui-date-picker (interop/react-factory EuiDatePicker))

(defsc Root [this props]
  (dom/div :.bp3-dark
           (ui-person @person)
           (ui-button {:onClick #(js/console.log "Hello")} "test")
           (ui-form-row {:label "Select a date"}
                        (ui-date-picker {:selected startDate
                                         :onChange #(js/console.log %)}))))

(defn ^:export init
  "Shadow-cljs sets this up to be our entry-point function. See shadow-cljs.edn `:init-fn` in the modules of the main build."
  []
  (app/mount! app Root "app")
  (js/console.log "Loaded"))

(defn ^:export refresh
  "During development, shadow-cljs will call this on every hot reload of source. See shadow-cljs.edn"
  []
  ;; re-mounting will cause forced UI refresh, update internals, etc.
  (app/mount! app Root "app")
  ;; As of Fulcro 3.3.0, this addition will help with stale queries when using dynamic routing:
  (comp/refresh-dynamic-queries! app)
  (js/console.log "Hot reload"))