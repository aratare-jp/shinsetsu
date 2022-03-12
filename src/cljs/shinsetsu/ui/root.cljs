(ns shinsetsu.ui.root
  (:require
    [shinsetsu.ui.elastic :as e]
    [shinsetsu.ui.login :refer [Login]]
    [shinsetsu.ui.main :refer [Main]]
    [com.fulcrologic.fulcro.dom :refer [div]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :refer [defrouter]]
    [com.fulcrologic.fulcro.mutations :as m]))

(defrouter RootRouter
  [_ {:keys [current-state]}]
  {:router-targets [Login Main]}
  (case current-state
    :pending (div "Loading...")
    :failed (div "Loading failed.")
    (div "Unknown route")))

(def ui-root-router (comp/factory RootRouter))

(defsc Root
  [this {:root/keys [router] :ui/keys [dark-mode?]}]
  {:query         [:root/ready? {:root/router (comp/get-query RootRouter)} :ui/dark-mode?]
   :initial-state (fn [_] {:root/router   (comp/get-initial-state RootRouter)
                           :ui/dark-mode? true})}
  (e/provider {:colorMode (if dark-mode? "dark" "light")}
    (ui-root-router router)))
