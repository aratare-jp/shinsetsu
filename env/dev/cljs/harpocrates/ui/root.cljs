(ns harpocrates.ui.root
  (:require
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
    [harpocrates.ui.login :refer [Login]]
    [harpocrates.ui.main :refer [Main]]
    [taoensso.timbre :as log]
    [harpocrates.ui.elastic-ui :refer [ui-control-bar]]))

(defn controls
  [comp]
  [{:controlType "text"
    :id          "dev_label"
    :text        "Dev Control Bar"}
   {:controlType "divider"}
   {:controlType "button"
    :id          "change_to_main"
    :label       "Move to Main Page"
    :color       "primary"
    :onClick     #(dr/change-route! comp ["main"])}
   {:controlType "button"
    :id          "change_to_login"
    :label       "Move to Login Page"
    :color       "primary"
    :onClick     #(dr/change-route! comp ["login"])}])

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
  (dom/div
    (ui-control-bar {:controls (controls this)})
    (ui-root-router router)))