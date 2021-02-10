(ns harpocrates.ui.tab
  (:require [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.components :refer [defsc] :as comp]
            [com.fulcrologic.fulcro.data-fetch :as df]
            [harpocrates.ui.bookmark :refer [Bookmark ui-bookmark]]
            [harpocrates.ui.elastic-ui :as eui]
            [taoensso.timbre :as log]
            [com.fulcrologic.fulcro.mutations :as m]
            [com.fulcrologic.fulcro.mutations :refer [defmutation]]))

(defsc Tab [this
            {:tab/keys [id name bookmarks] :as props}]
  {:query         [:tab/id
                   :tab/name
                   {:tab/bookmarks (comp/get-query Bookmark)}]
   :ident         :tab/id
   :initial-state {:tab/id        :invalid
                   :tab/name      :invalid
                   :tab/bookmarks []}}
  (dom/ul
    (map #(ui-bookmark %) bookmarks)))

(def ui-tab (comp/factory Tab {:keyfn :tab/id}))

(defsc TabList
  [this {:user/keys [tabs] :ui/keys [selected-tab]}]
  {:query         [:ui/selected-tab
                   {:user/tabs (comp/get-query Tab)}]
   :initial-state (fn [p] {:ui/selected-tab :invalid})
   :ident         (fn [] [:component/id :tab-list])}
  (let [tabs           (mapv (fn [{:tab/keys [id name bookmarks]}] {:id      (str "tab" id)
                                                                    :name    name
                                                                    :content (ui-tab
                                                                               {:tab/id        id
                                                                                :tab/name      name
                                                                                :tab/bookmarks bookmarks})})
                             tabs)
        select-handler (fn [obj] (m/set-string! this :ui/selected-tab :value (-> obj (js->clj :keywordize-keys true) :id)))]
    (eui/ui-tabbed-content
      {:tabs               tabs
       :initialSelectedTab (if (not= selected-tab :invalid)
                             (first (filter #(= selected-tab (:tab/id %)) tabs))
                             (first tabs))
       :autoFocus          "selected"
       :onTabClick         select-handler})))

(def ui-tab-list (comp/factory TabList))

(comment
  (def s (com.fulcrologic.fulcro.application/current-state harpocrates.application/app))
  (def tabs (com.fulcrologic.fulcro.algorithms.denormalize/db->tree
              [{:session/current-user [{:user/tabs ['*]}]}]
              s
              s))
  tabs
  (map (fn [[k {:tab/keys [id name bookmarks]}]] {:id      id
                                                  :name    name
                                                  :content (harpocrates.ui.tab/ui-tab
                                                             {:id        id
                                                              :name      name
                                                              :bookmarks bookmarks})})
       (get-in tabs [:user/current-user :user/tabs])))