(ns harpocrates.ui.main
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.routing.dynamic-routing :refer [route-immediate]]))

(defsc Main
  [_ _]
  {:ident        [:component/id :main]
   :route-segment ["main"]
   :query         [:main]
   :initial-state {:main {}}
   :will-enter    (fn [_ _] (route-immediate [:component/id :main]))}
  (dom/h1 "Main!"))

(def ui-main (comp/factory Main))