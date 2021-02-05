(ns harpocrates.ui.root
  (:require
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :refer [defrouter]]
    [harpocrates.ui.login :refer [Login]]
    [harpocrates.ui.main :refer [Main]]
    [harpocrates.ui.elastic-ui :refer [ui-button]]))

(defrouter RootRouter [_ {:keys [current-state]}]
  {:router-targets [Login Main]}
  (case current-state
    :pending (dom/div "Loading...")
    :failed (dom/div "Loading seems to have failed. Try another route.")
    (dom/div "Unknown route")))

(def ui-root-router (comp/factory RootRouter))

(defsc Root [this {:root/keys [router]}]
  {:query         [{:root/router (comp/get-query RootRouter)}]
   :initial-state {:root/router  {}
                   :component/id {:main  (comp/get-initial-state Main)
                                  :login (comp/get-initial-state Login)}}}
  (dom/div (ui-root-router router)))