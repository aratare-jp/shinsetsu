(ns harpocrates.ui.main
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.routing.dynamic-routing :refer [route-immediate]]
    [com.fulcrologic.fulcro.application :refer [current-state]]
    [harpocrates.application :refer [app]]
    [taoensso.timbre :as log]
    [harpocrates.ui.tab :refer [ui-tab-list TabList]]))

(defsc Main
  [this {:keys [ui/is-loading?] :as props}]
  {:ident         (fn [] [:component/id :main])
   :query         [:ui/is-loading?]
   :initial-state {:ui/is-loading? false}
   :route-segment ["main"]
   :will-enter    (fn [_ _] (route-immediate [:component/id :main]))}
  (let [app-state (current-state app)
        user-tabs (log/spy :info (get-in app-state [:session/current-user :user/tabs]))]
    (dom/div
      (dom/h1 "Main!")
      (ui-tab-list {:user/tabs user-tabs}))))

(def ui-main (comp/factory Main))