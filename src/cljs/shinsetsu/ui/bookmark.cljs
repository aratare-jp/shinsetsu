(ns shinsetsu.ui.bookmark
  (:require
    [clojure.browser.dom :refer [get-element]]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.dom :refer [div h1 p]]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.fulcro.networking.file-upload :as fu]
    [goog.functions :as gf]
    [lambdaisland.deep-diff2 :refer [diff]]
    [malli.core :as mc]
    [shinsetsu.mutations.bookmark :refer [create-bookmark delete-bookmark patch-bookmark]]
    [shinsetsu.mutations.tag :refer [post-options-load]]
    [shinsetsu.schema :as s]
    [shinsetsu.ui.elastic :as e]
    [shinsetsu.ui.tag :as tui]))

(defsc BookmarkModal
  [this
   {:bookmark/keys [id title url tab-id tags]
    :ui/keys       [image loading? error-type tag-options tags-loading?] :as props}
   {:keys [on-close]}]
  {:ident         :bookmark/id
   :query         (fn []
                    [:bookmark/id
                     :bookmark/title
                     :bookmark/url
                     :bookmark/image
                     :bookmark/tab-id
                     :bookmark/favourite
                     {:bookmark/tags (comp/get-query tui/TagModal)}
                     :ui/bookmark-ctx-menu-id
                     (let [TabModal (comp/registry-key->class `shinsetsu.ui.tab/TabModal)]
                       {[:ui/tabs '_] (comp/get-query TabModal)})
                     :ui/loading?
                     :ui/error-type
                     :ui/image
                     :ui/show-ctx-menu?
                     {:ui/tag-options (comp/get-query tui/TagModal)}
                     :ui/tags-loading?
                     fs/form-config-join])
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
                             (let [tags (->> (fs/dirty-fields props false) vals first :bookmark/tags (mapv second))
                                   args #:bookmark{:id id :title title :url url :tab-id tab-id :add-tags tags}]
                               (if image
                                 (comp/transact! this [{(create-bookmark (fu/attach-uploads args image)) (comp/get-query this)}])
                                 (comp/transact! this [{(create-bookmark args) (comp/get-query this)}])))
                             (let [{:keys [before after]} (->> (fs/dirty-fields props true) vals first :bookmark/tags)
                                   tag-diff    (diff (mapv second before) (mapv second after))
                                   add-tags    (->> tag-diff (mapv :+) (filterv some?))
                                   remove-tags (->> tag-diff (mapv :-) (filterv some?))
                                   args        (-> props
                                                   (fs/dirty-fields false)
                                                   vals
                                                   first
                                                   (merge #:bookmark{:id id :tab-id tab-id})
                                                   (dissoc :bookmark/tags)
                                                   (assoc :bookmark/add-tags add-tags :bookmark/remove-tags remove-tags))]
                               (if image
                                 (comp/transact! this [{(patch-bookmark (fu/attach-uploads args image)) (comp/get-query this)}])
                                 (comp/transact! this [{(patch-bookmark args) (comp/get-query this)}])))))
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
               :placeholder     "Start adding tags by searching"
               :isDisabled      loading?
               :isLoading       tags-loading?
               :async           true
               :selectedOptions (map
                                  (fn [{:tag/keys [id name colour]}]
                                    {:key (str "tag-option-" id) :label name :color colour :value id})
                                  tags)
               :options         (map
                                  (fn [{:tag/keys [id name colour]}]
                                    {:key (str "tag-option-" id) :label name :color colour :value id})
                                  tag-options)
               :onSearchChange  (gf/debounce
                                  (fn [name-tag]
                                    (if (not (empty? name-tag))
                                      (do
                                        (m/set-value! this :ui/tags-loading? true)
                                        (df/load-field! this :ui/tag-options {:update-query         (fn [q]
                                                                                                      [{:user/tags (-> q
                                                                                                                       first
                                                                                                                       :ui/tag-options)}])
                                                                              :params               {:tag/name name-tag}
                                                                              :post-mutation        `post-options-load
                                                                              :post-mutation-params {:ident (comp/get-ident this)}}))))
                                  500)
               :onChange        (fn [opts]
                                  (let [tags (as-> opts $
                                                   (js->clj $ :keywordize-keys true)
                                                   (mapv
                                                     (fn [{:keys [label color value]}]
                                                       #:tag{:name label :colour color :id value})
                                                     $))]
                                    (m/set-value! this :ui/tag-options nil)
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
   {:bookmark/keys [id title image favourite url tab-id tags]
    bcmi           :ui/bookmark-ctx-menu-id}
   {:keys [on-edit on-delete on-move]}]
  {:ident :bookmark/id
   :query [:bookmark/id
           :bookmark/title
           :bookmark/url
           :bookmark/favourite
           :bookmark/image
           :bookmark/tab-id
           {:bookmark/tags (comp/get-query tui/TagModal)}
           :ui/bookmark-ctx-menu-id
           :ui/image
           :ui/show-ctx-menu?
           :ui/tags-loading?
           {:ui/tag-options (comp/get-query tui/TagModal)}]}
  (let [close-ctx-menu-fn  #(m/set-value! this :ui/bookmark-ctx-menu-id nil)
        on-favourite       (fn []
                             (comp/transact! this [(patch-bookmark #:bookmark{:id id :tab-id tab-id :favourite (not favourite)})])
                             (close-ctx-menu-fn))
        on-delete          (fn []
                             (on-delete)
                             (close-ctx-menu-fn))
        on-edit            (fn []
                             (on-edit)
                             (close-ctx-menu-fn))
        on-move            (fn []
                             (on-move #:bookmark{:id id :tab-id tab-id :title title})
                             (close-ctx-menu-fn))
        on-open-in-new-tab (fn []
                             (js/window.open url)
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
                 {:aria-label "open in new tab"
                  :size       "s"
                  :iconType   "popout"
                  :onClick    on-open-in-new-tab}))
             (e/flex-item {}
               (e/button-icon
                 {:aria-label "edit"
                  :size       "s"
                  :iconType   "pencil"
                  :onClick    on-edit}))
             (e/flex-item {}
               (e/button-icon
                 {:aria-label "move"
                  :size       "s"
                  :iconType   "merge"
                  :onClick    on-move}))
             (e/flex-item {}
               (e/button-icon
                 {:aria-label "delete"
                  :size       "s"
                  :iconType   "trash"
                  :onClick    on-delete
                  :color      "danger"}))))))
     (e/card
       {:title          title
        :titleElement   "h2"
        :description    (map (fn [{:tag/keys [name colour]}] (e/badge {:color colour} name)) tags)
        :paddingSize    "s"
        :display        "transparent"
        :betaBadgeProps (if favourite {:label (e/icon {:type "starFilled" :size "l" :color "#F3D371" :title "favourite"})})
        :onClick        #(set! (.. js/window -location -href) url)
        :onContextMenu  (fn [e]
                          (evt/prevent-default! e)
                          (if bcmi
                            (m/set-value! this :ui/bookmark-ctx-menu-id nil)
                            (m/set-value! this :ui/bookmark-ctx-menu-id id)))
        :image          (div
                          (div {:id (str "bookmark-" id)})
                          (e/image
                            {:height "200vh"
                             :src    image}))})]))

(def ui-bookmark (comp/factory Bookmark {:keyfn :bookmark/id}))

(defn ui-loading-bookmark
  []
  (e/card
    {:title       (e/loading-content {:lines 1})
     :description (e/loading-content {:lines 1})
     :paddingSize "l"
     :image       (e/image {:height "200vh"})}))

(defn ui-new-bookmark
  [props]
  (let [{:keys [on-edit]} (comp/get-computed props)]
    (e/card
      {:title        "New Bookmark"
       :titleElement "h2"
       :paddingSize  "l"
       :display      "transparent"
       :image        (e/image {:height "200vh"})
       :icon         (e/icon {:size "xxl" :type "plus"})
       :onClick      on-edit})))

(defn ui-new-bookmark-modal
  [this {:tab/keys [id bookmarks] :ui/keys [edit-bm-id]}]
  (let [bookmark (->> bookmarks (filter #(= edit-bm-id (:bookmark/id %))) first (merge {:bookmark/tab-id id}))
        on-close #(m/set-value! this :ui/edit-bm-id nil)]
    (ui-bookmark-modal (comp/computed bookmark {:on-close on-close}))))

(defn ui-edit-bookmark-modal
  [this {:tab/keys [id bookmarks] :ui/keys [edit-bm-id]}]
  (let [bookmark (->> bookmarks (filter #(= edit-bm-id (:bookmark/id %))) first (merge {:bookmark/tab-id id}))
        on-close #(m/set-value! this :ui/edit-bm-id nil)]
    (ui-bookmark-modal (comp/computed bookmark {:on-close on-close}))))

(defn ui-delete-bookmark-modal
  [this {:ui/keys [delete-bm-id] :tab/keys [bookmarks] tab-id :tab/id}]
  (let [{:bookmark/keys [id title]} (->> bookmarks (filter #(= delete-bm-id (:bookmark/id %))) first)]
    (e/confirm-modal
      {:title             (str "Delete bookmark " title)
       :onCancel          #(m/set-value! this :ui/delete-bm-id nil)
       :onConfirm         #(comp/transact! this [(delete-bookmark #:bookmark{:id id :tab-id tab-id})])
       :cancelButtonText  "Cancel"
       :confirmButtonText "Yes, I'm sure!"
       :buttonColor       "danger"}
      (p "Delete this bookmark will permanently remove it from this tab")
      (p "Are you sure you want to delete this bookmark?"))))

(defn ui-move-bookmark-modal
  [this {:ui/keys [tabs destination-tab-id move-bm-id]}]
  (let [{:bookmark/keys [id tab-id title]} move-bm-id
        tabs   (filter #(not= (:tab/id %) tab-id) tabs)
        tab-id (as-> destination-tab-id $
                     (or $ (-> tabs first :tab/id))
                     (if (uuid? $) $ (uuid $)))]
    (e/confirm-modal
      {:title             (str "Move bookmark " title)
       :onCancel          #(m/set-value! this :ui/move-bm-id nil)
       :onConfirm         #(comp/transact! this [{(patch-bookmark #:bookmark{:id id :tab-id tab-id}) (comp/get-query Bookmark)}])
       :cancelButtonText  "Cancel"
       :confirmButtonText "Yes, I'm sure!"}
      (p "Please select your destination tab")
      (e/select {:options  (mapv (fn [{:tab/keys [id name]}] {:text name :value id}) tabs)
                 :value    tab-id
                 :onChange #(m/set-value! this :ui/destination-tab-id (uuid (evt/target-value %)))}))))
