(ns shinsetsu.ui.main
  (:require
    [shinsetsu.mutations :as api]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom :refer [div label input form button]]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]))

(defsc Main
  [this props]
  {:ident         (fn [] [:component/id :main])
   :route-segment ["main"]
   :query         []
   :initial-state {}}
  (div "Hello!"))

(def ui-main (comp/factory Main))
