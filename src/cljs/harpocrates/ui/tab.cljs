(ns harpocrates.ui.tab
  (:require [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.components :refer [defsc] :as comp]
            [harpocrates.ui.elastic-ui :as eui]
            [harpocrates.ui.bookmark :refer [ui-bookmark-card BookmarkCard]]
            [taoensso.timbre :as log]
            [com.fulcrologic.fulcro.mutations :as m]
            [com.fulcrologic.fulcro.mutations :refer [defmutation]]
            [harpocrates.routing :as routing]))

(defsc Tab [_ {:tab/keys [bookmarks]}]
  {:query [:tab/id :tab/name {:tab/bookmarks (comp/get-query BookmarkCard)}]
   :ident :tab/id}
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
            (map #(ui-bookmark-card %) bookmarks)))))))

(def ui-tab (comp/factory Tab {:keyfn :tab/id}))

(defsc TabList [this {:ui/keys [selected-tab tabs]}]
  {:query         [:ui/selected-tab {:ui/tabs (comp/get-query Tab)}]
   :initial-state (fn [_] {:ui/selected-tab :invalid})
   :ident         (fn [] [:component/id :tab-list])}
  (let [tabs           (conj tabs {:tab/id "tab-plus" :tab/name (eui/ui-icon {:type "plus"})})
        select-handler (fn [id] (m/set-string! this :ui/selected-tab :value id))
        selected-tab   (if (= :invalid selected-tab) (-> tabs first :tab/id) selected-tab)]
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