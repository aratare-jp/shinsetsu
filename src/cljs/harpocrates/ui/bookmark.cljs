(ns harpocrates.ui.bookmark
  (:require [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.components :refer [defsc] :as comp]
            [com.fulcrologic.fulcro.data-fetch :as df]
            [taoensso.timbre :as log]))

(defsc Bookmark [_ {:bookmark/keys [id url] :as props}]
  {:query [:bookmark/id :bookmark/url]
   :ident :bookmark/id}
  (dom/div
    (dom/h1 "URL:")
    (dom/h1 url)))

(def ui-bookmark (comp/factory Bookmark {:keyfn :bookmark/id}))
