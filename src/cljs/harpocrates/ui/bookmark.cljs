(ns harpocrates.ui.bookmark
  (:require [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.components :refer [defsc] :as comp]
            [com.fulcrologic.fulcro.routing.dynamic-routing :refer-macros [defrouter] :as dr]
            [harpocrates.routing :refer [route-to!]]
            [com.fulcrologic.fulcro.data-fetch :as df]
            [harpocrates.ui.elastic-ui :as eui]
            [taoensso.timbre :as log]
            [com.fulcrologic.fulcro.mutations :as m]))

(defsc Bookmark [_ {:bookmark/keys [name url]}]
  {:ident :bookmark/id
   :query [:bookmark/id :bookmark/name :bookmark/url]}
  (eui/ui-form
    {:component "form"}
    (eui/ui-form-row
      {:label "Name" :fullWidth true}
      (eui/ui-field-text {:value name}))
    (eui/ui-form-row
      {:label "URL" :fullWidth true}
      (eui/ui-field-text {:value url}))))

(def ui-bookmark (comp/factory Bookmark))

(defsc BookmarkCard [this {:bookmark/keys [name url] :ui/keys [show-bookmark-modal?] :as props}]
  {:query [:bookmark/id :bookmark/url :bookmark/name :ui/show-bookmark-modal?]
   :ident :bookmark/id}
  (let [open-modal  #(m/set-value! this :ui/show-bookmark-modal? true)
        close-modal #(m/set-value! this :ui/show-bookmark-modal? false)]
    [(eui/ui-flex-item
       nil
       (dom/div
         (eui/ui-card
           {:image       "https://source.unsplash.com/400x200/?Nature"
            :title       name
            :description "Hello world"
            :onClick     open-modal})))
     (if show-bookmark-modal?
       (eui/ui-overlay-mask
         {:onClick close-modal}
         (eui/ui-modal
           {:onClose close-modal}
           (eui/ui-modal-header nil (eui/ui-modal-header-title nil "Bookmark"))
           (eui/ui-modal-body nil (ui-bookmark {:bookmark/name name :bookmark/url url}))
           (eui/ui-modal-footer
             nil
             (eui/ui-button-empty {:onClick close-modal} "Cancel")
             (eui/ui-button {:onClick close-modal :fill true} "Save")))))]))

(def ui-bookmark-card (comp/factory BookmarkCard {:keyfn :bookmark/id}))