(ns harpocrates.ui.main
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.application :refer [current-state]]
    [harpocrates.application :refer [app]]
    [com.fulcrologic.fulcro.algorithms.denormalize :refer [db->tree]]
    [taoensso.timbre :as log]
    [harpocrates.ui.elastic-ui :refer [ui-button]]
    [harpocrates.ui.tab :refer [ui-tab-list TabList Tab]]
    [harpocrates.ui.user :refer [CurrentUser]]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.mutations :refer-macros [defmutation]]))

(defsc Main
  [_ {:ui/keys [tab-list] :session/keys [current-user]}]
  {:ident         (fn [] [:component/id :main])
   :query         [{:ui/tab-list (comp/get-query TabList)}
                   {[:session/current-user '_] (comp/get-query CurrentUser)}]
   :initial-state (fn [_] {:ui/tab-list (comp/get-initial-state TabList)})
   :route-segment ["main"]
   :will-enter    (fn [_ _] (dr/route-immediate [:component/id :main]))}
  (dom/div (ui-tab-list (assoc tab-list :ui/tabs (:user/tabs current-user)))))

(def ui-main (comp/factory Main))