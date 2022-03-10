(ns shinsetsu.ui.main
  (:require
    [shinsetsu.mutations :as api]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom :refer [div label input form button]]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.data-fetch :as df]))

(defsc Child
  [this {:tab/keys [id] :as tab}]
  {:ident (fn [] [:tab/id (:tab/id tab)])
   :query [:tab/id :tab/name :tab/created :tab/updated]}
  (div tab))

(def ui-child (comp/factory Child))

(defsc Main
  [this props]
  {:ident         (fn [] [:component/id :main])
   :route-segment ["main"]
   :query         [{:tab/ids (comp/get-query Child)}]
   :initial-state {}}
  (button {:onClick #(df/load! this :tab/ids Child {:remote :protected})} "Click!"))

(def ui-main (comp/factory Main))
