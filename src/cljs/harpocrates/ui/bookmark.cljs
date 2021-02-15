(ns harpocrates.ui.bookmark
  (:require [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.components :refer [defsc] :as comp]
            [com.fulcrologic.fulcro.routing.dynamic-routing :refer-macros [defrouter] :as dr]
            [harpocrates.routing :refer [route-to!]]
            [com.fulcrologic.fulcro.data-fetch :as df]
            [harpocrates.ui.elastic-ui :as eui]
            [taoensso.timbre :as log]
            [com.fulcrologic.fulcro.mutations :as m]))

(defsc Bookmark [this {:bookmark/keys [id name url]}]
  {:ident         :bookmark/id
   :query         [:bookmark/id :bookmark/name :bookmark/url]
   :route-segment ["bookmark" :bookmark/id]
   :will-enter    (fn [app {:bookmark/keys [id] :as route-params}]
                    (dr/route-deferred
                      [:bookmark/id id]
                      #(df/load! app [:bookmark/id id] Bookmark
                                 {:post-mutation        `dr/target-ready
                                  :post-mutation-params {:target [:bookmark/id id]}})))}
  (dom/div
    (eui/ui-button {:onClick #(route-to! "/main")} "Back")
    (eui/ui-form
      {:component "form"}
      (eui/ui-form-row
        {:label "Name" :fullWidth true}
        (eui/ui-field-text {:value name}))
      (eui/ui-form-row
        {:label "URL" :fullWidth true}
        (eui/ui-field-text {:value url}))
      (eui/ui-button-empty {:onClick #(route-to! "/main")} "Cancel")
      (eui/ui-button {} "Save"))))
