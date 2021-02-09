(ns harpocrates.ui.tab
  (:require [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.components :refer [defsc] :as comp]
            [com.fulcrologic.fulcro.data-fetch :as df]))

(defsc Tab [_ {:tab/keys [name bookmarks]}]
  {:query [:tab/id :tab/name :tab/bookmarks]
   :ident :tab/id}
  (dom/div
    (dom/h1 "Name:")
    (dom/div name)
    (dom/h1 "Bookmarks:")
    (dom/ul
      (map #(dom/li %) bookmarks))))

(def ui-tab (comp/factory Tab {:keyfn :tab/id}))

(defsc TabList [_ {:keys [user/tabs]}]
  (dom/div
    (dom/h1 "Tab List")
    (dom/ul
      (map #(ui-tab %) tabs))))

(def ui-tab-list (comp/factory TabList))
