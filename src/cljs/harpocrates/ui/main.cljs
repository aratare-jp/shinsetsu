(ns harpocrates.ui.main
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.routing.dynamic-routing :refer [route-immediate]]))

(defsc Main
  [_ {:keys [is-loading?] :as props}]
  {:ident         (fn [] [:component/id :main])
   :query         [:is-loading?]
   :initial-state {:is-loading? false}
   :route-segment ["main"]
   :will-enter    (fn [_ _] (route-immediate [:component/id :main]))}
  (println (str "Main props: " props))
  (dom/h1 "Main!"))

(def ui-main (comp/factory Main))