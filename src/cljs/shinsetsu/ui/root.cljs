(ns shinsetsu.ui.root
  (:require
    [shinsetsu.ui.login :refer [Login]]
    [shinsetsu.ui.main :refer [Main]]
    [com.fulcrologic.fulcro.dom :refer [div]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :refer [defrouter]]))

(defrouter RootRouter
  [_ {:keys [current-state]}]
  {:router-targets [Login Main]}
  (case current-state
    :pending (div "Loading...")
    :failed (div "Loading failed.")
    (div "Unknown route")))

(def ui-root-router (comp/factory RootRouter))

(defsc Root
  [_ {:root/keys [router]}]
  {:query         [:root/ready? {:root/router (comp/get-query RootRouter)}]
   :initial-state {:root/router {}}}
  (ui-root-router router))
