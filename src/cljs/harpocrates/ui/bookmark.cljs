(ns harpocrates.ui.bookmark
  (:require [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.components :refer [defsc] :as comp]
            [com.fulcrologic.fulcro.data-fetch :as df]
            [harpocrates.ui.elastic-ui :as eui]
            [taoensso.timbre :as log]))

(defsc Bookmark [_ {:bookmark/keys [id name url] :as props}]
  {:query [:bookmark/id :bookmark/url :bookmark/name]
   :ident :bookmark/id}
  (eui/ui-flex-item
    nil
    (eui/ui-card
      {:image       "https://source.unsplash.com/400x200/?Nature"
       :title       name
       :description "Hello world"
       :onClick     #(println "Hello")})))

(def ui-bookmark (comp/factory Bookmark {:keyfn :bookmark/id}))
