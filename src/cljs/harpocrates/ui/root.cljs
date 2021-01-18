(ns harpocrates.ui.root
  (:require
    [harpocrates.mutations :as api]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
    [com.fulcrologic.fulcro.ui-state-machines :as uism]
    [harpocrates.ui.login :refer [Login]]
    [harpocrates.ui.main :refer [Main]]))

(defrouter RootRouter [_ {:keys [current-state]}]
  {:router-targets [Login Main]}
  (case current-state
    :pending (dom/div "Loading...")
    :failed (dom/div "Loading seems to have failed. Try another route.")
    (dom/div "Unknown route")))

(def ui-root-router (comp/factory RootRouter))

(defsc Root [this {:root/keys [router]}]
  {:query         [{:root/router (comp/get-query RootRouter)}]
   :initial-state {:root/router {}}}
  (dom/div (ui-root-router router)))