(ns shinsetsu.ui.bookmark
  (:require
    [shinsetsu.application :refer [app]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.dom :refer [h1 div]]
    [com.fulcrologic.fulcro.mutations :as m]
    [clojure.browser.dom :refer [get-element]]
    [malli.core :as mc]
    [shinsetsu.schema :as s]
    [shinsetsu.mutations.bookmark :refer [create-bookmark patch-bookmark delete-bookmark]]
    [shinsetsu.ui.elastic :as e]
    [shinsetsu.ui.tag :as tui]
    [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.fulcro.networking.file-upload :as fu]
    [taoensso.timbre :as log]
    [shinsetsu.mutations.common :refer [set-root]]))

(defsc BookmarkModal
  [this
   {:bookmark/keys [id title url tab-id tags]
    all-tags       :root/tags
    :ui/keys       [image loading? error-type] :as props}
   {:keys [on-close]}]
  {:ident         :bookmark/id
   :query         [{[:root/tags '_] (comp/get-query tui/TagModal)}
                   [:root/bookmark-ctx-menu-id '_]
                   :bookmark/id
                   :bookmark/title
                   :bookmark/url
                   :bookmark/image
                   :bookmark/tab-id
                   :bookmark/favourite
                   {:bookmark/tags (comp/get-query tui/TagModal)}
                   :ui/loading?
                   :ui/error-type
                   :ui/image
                   :ui/show-ctx-menu?
                   fs/form-config-join]
   :form-fields   #{:bookmark/title :bookmark/url :bookmark/image :bookmark/tags}
   :initial-state (fn [{:bookmark/keys [tab-id]}]
                    #:bookmark{:id     (tempid/tempid)
                               :title  ""
                               :url    ""
                               :tab-id tab-id})
   :pre-merge     (fn [{:keys [data-tree]}] (fs/add-form-config BookmarkModal data-tree))}
  (let [on-blur          (fn [f] (comp/transact! this [(fs/mark-complete! {:field f})]))
        on-file-selected (fn [fs]
                           (let [files (mapv #(fu/new-upload (.-name %) % (.-type %)) fs)]
                             (m/set-value! this :ui/image files)))
        bm-valid?        (mc/validate s/bookmark-form-spec #:bookmark{:title title :url url})
        on-close         (fn [_]
                           (comp/transact! this [(fs/reset-form! {:form-ident (comp/get-ident this)})])
                           (if on-close (on-close)))
        on-save          (fn [e]
                           (evt/prevent-default! e)
                           (if (tempid/tempid? id)
                             (let [args #:bookmark{:id id :title title :url url :tab-id tab-id}]
                               (if image
                                 (comp/transact! this [(create-bookmark (fu/attach-uploads args image))])
                                 (comp/transact! this [(create-bookmark args)])))
                             (let [args (-> props
                                            (fs/dirty-fields false)
                                            vals
                                            first
                                            (merge #:bookmark{:id id :tab-id tab-id})
                                            (update :bookmark/tags #(mapv second %)))]
                               (if image
                                 (comp/transact! this [(patch-bookmark (fu/attach-uploads args image))])
                                 (comp/transact! this [(patch-bookmark args)])))))
        on-clear         #(comp/transact! this [(fs/reset-form! {:form-ident (comp/get-ident this)})])
        errors           (case error-type
                           :invalid-input ["Unable to create new tab." "Please try again."]
                           :internal-server-error ["Unknown error encountered"]
                           nil)]
    (e/modal {:onClose on-close}
      (e/modal-header {}
        (e/modal-header-title {}
          (h1 (if (tempid/tempid? id) "Create New Bookmark" "Edit Bookmark"))))
      (e/modal-body {}
        (e/form {:id "bookmark-modal-form" :component "form" :isInvalid (boolean errors) :error errors}
          (e/form-row {:label "Title"}
            (e/field-text
              {:name     "title"
               :value    title
               :onChange (fn [e] (m/set-string! this :bookmark/title :event e))
               :onBlur   #(on-blur :bookmark/title)
               :disabled loading?}))
          (e/form-row {:label "URL"}
            (e/field-text
              {:name     "url"
               :value    url
               :onBlur   #(on-blur :bookmark/url)
               :disabled loading?
               :onChange (fn [e] (m/set-string! this :bookmark/url :event e))}))
          (e/form-row {:label "Image"}
            (e/file-picker {:onChange on-file-selected :accept "image/*"}))
          (e/form-row {:label "Tags"}
            (e/combo-box
              {:aria-label      "Select tag for bookmark"
               :placeholder     "Select tags"
               :selectedOptions (map
                                  (fn [{:tag/keys [id name colour]}]
                                    {:label name :color colour :value id})
                                  tags)
               :options         (map (fn [{:tag/keys [id name colour]}]
                                       {:label name :color colour :value id})
                                     all-tags)
               :onChange        (fn [os]
                                  (let [tags (as-> os $
                                                   (js->clj $ :keywordize-keys true)
                                                   (mapv
                                                     (fn [{:keys [label color value]}]
                                                       #:tag{:name   label
                                                             :colour color
                                                             :id     value})
                                                     $))]
                                    (m/set-value! this :bookmark/tags tags)))}))))
      (e/modal-footer {}
        (e/flex-group {}
          (e/flex-item {}
            (e/button {:onClick on-close} "Cancel"))
          (e/flex-item {}
            (e/button
              {:type      "submit"
               :fill      true
               :onClick   on-save
               :isLoading loading?
               :disabled  (not bm-valid?)
               :iconType  "save"
               :form      "bookmark-modal-form"}
              "Save"))
          (e/flex-item {}
            (e/button {:onClick on-clear} "Clear")))))))

(def ui-bookmark-modal (comp/factory BookmarkModal {:keyfn :bookmark/id}))

(defsc Bookmark
  [this
   {:bookmark/keys [id title image favourite url tab-id tags] bcmi :root/bookmark-ctx-menu-id}
   {:keys [on-click]}]
  {:ident :bookmark/id
   :query [[:root/bookmark-ctx-menu-id '_]
           :bookmark/id
           :bookmark/title
           :bookmark/url
           :bookmark/favourite
           :bookmark/image
           :bookmark/tab-id
           {:bookmark/tags (comp/get-query tui/TagModal)}
           :ui/image
           :ui/show-ctx-menu?]}
  (let [close-ctx-menu-fn #(comp/transact! this [(set-root {:k :root/bookmark-ctx-menu-id :v nil})])
        on-favourite      (fn []
                            (comp/transact! this [(patch-bookmark #:bookmark{:id        id
                                                                             :tab-id    tab-id
                                                                             :favourite (not favourite)})])
                            (close-ctx-menu-fn))
        on-delete         (fn []
                            (comp/transact! this [(delete-bookmark #:bookmark{:id id :tab-id tab-id})])
                            (close-ctx-menu-fn))
        on-edit           (fn []
                            (on-click)
                            (close-ctx-menu-fn))]
    [(if (= id bcmi)
       (let [el (get-element (str "bookmark-" id))]
         (e/wrapping-popover
           {:button           el
            :initialFocus     false
            :anchorPosition   "upCenter"
            :panelPaddingSize "s"
            :isOpen           (= id bcmi)
            :closePopover     close-ctx-menu-fn}
           (e/flex-group {:gutterSize "none"}
             (e/flex-item {}
               (e/button-icon
                 {:aria-label "favourite"
                  :size       "s"
                  :iconType   (if favourite "starFilled" "starEmpty")
                  :onClick    on-favourite}))
             (e/flex-item {}
               (e/button-icon
                 {:aria-label "edit"
                  :size       "s"
                  :iconType   "pencil"
                  :onClick    on-edit}))
             (e/flex-item {}
               (e/button-icon
                 {:aria-label "delete"
                  :size       "s"
                  :iconType   "trash"
                  :onClick    on-delete
                  :color      "danger"}))))))
     (e/card
       {:title         title
        :titleElement  "h2"
        :description   (map (fn [{:tag/keys [name colour]}] (e/badge {:color colour} name)) tags)
        :display       "subdued"
        :onClick       #(js/window.open url)
        :onContextMenu (fn [e]
                         (evt/prevent-default! e)
                         (comp/transact! this [(set-root {:k :root/bookmark-ctx-menu-id :v id})]))
        :image         (div
                         (div {:id (str "bookmark-" id)})
                         (e/image
                           {:height "200vh"
                            :src    image}))})]))

(def ui-bookmark (comp/factory Bookmark {:keyfn :bookmark/id}))

(defsc NewBookmark
  [_ _ {:keys [on-click]}]
  {}
  (e/card
    {:title        "Add New"
     :titleElement "h2"
     :paddingSize  "l"
     :display      "transparent"
     :image        (e/image {:height "200vh"})
     :icon         (e/icon {:size "xxl" :type "plus"})
     :onClick      on-click}))

(def ui-new-bookmark (comp/factory NewBookmark))
