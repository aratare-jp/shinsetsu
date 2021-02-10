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
  (eui/ui-page
    nil
    (eui/ui-page-body
      nil
      (eui/ui-page-content
        nil
        (eui/ui-page-content-body
          nil
          (eui/ui-flex-grid
            {:gutterSize "l"
             :columns    4}
            (map #(ui-bookmark %) bookmarks)))))))

(def ui-tab (comp/factory Tab {:keyfn :tab/id}))

(defsc TabList
  [this {:user/keys [tabs] :ui/keys [selected-tab]}]
  {:query         [:ui/selected-tab
                   {:user/tabs (comp/get-query Tab)}]
   :initial-state (fn [p] {:ui/selected-tab :invalid})
   :ident         (fn [] [:component/id :tab-list])}
  (let [tabs           (conj tabs {:tab/id   "tab-plus"
                                   :tab/name (eui/ui-icon {:type "plus"})})
        select-handler (fn [id] (m/set-string! this :ui/selected-tab :value id))]
    [(eui/ui-tabs
       nil
       (map (fn [{:tab/keys [id name]}]
              (eui/ui-tab
                {:onClick    #(select-handler id)
                 :isSelected (= id selected-tab)
                 :key        id}
                name))
            tabs))
     (if (= selected-tab "tab-plus")
       (dom/div "Creating new here")
       (ui-tab (first (filter #(= (:tab/id %) selected-tab) tabs))))]))

(def ui-tab-list (comp/factory TabList))