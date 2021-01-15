(ns harpocrates.ui.main
  (:require
    [harpocrates.mutations :as api]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.routing.dynamic-routing :refer [route-immediate]]))

(defsc Main
  [_ _]
  {:route-segment ["main"]
   :query         [:main]
   :initial-state {:main "blug!"}
   :indent        [:component/id ::main]
   :will-enter    (fn [_ _] (route-immediate [:component/id ::main]))}
  (dom/h1 "Main!"))

(def ui-main (comp/factory Main))