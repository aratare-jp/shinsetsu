(ns shinsetsu.ui.bookmark
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.dom :refer [h1 div]]
    [com.fulcrologic.fulcro.mutations :as m]
    [malli.core :as mc]
    [shinsetsu.schema :as s]
    [shinsetsu.mutations.bookmark :refer [create-bookmark patch-bookmark delete-bookmark]]
    [shinsetsu.ui.elastic :as e]
    [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.fulcro.networking.file-upload :as fu]
    [taoensso.timbre :as log]))

(defsc BookmarkModal
  [this
   {:bookmark/keys [id title url tab-id]
    :ui/keys       [image loading? error-type] :as props}
   {:keys [on-close]}]
  {:ident         :bookmark/id
   :query         [:bookmark/id
                   :bookmark/title
                   :bookmark/url
                   :bookmark/image
                   :bookmark/tab-id
                   :bookmark/favourite
                   :ui/loading?
                   :ui/error-type
                   :ui/image
                   fs/form-config-join]
   :form-fields   #{:bookmark/title :bookmark/url :bookmark/image}
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
        on-save          (fn []
                           (if (tempid/tempid? id)
                             (let [args #:bookmark{:id id :title title :url url :tab-id tab-id}]
                               (if image
                                 (comp/transact! this [(create-bookmark (fu/attach-uploads args image))])
                                 (comp/transact! this [(create-bookmark args)])))
                             (let [args (-> props
                                            (fs/dirty-fields false)
                                            vals
                                            first
                                            (merge #:bookmark{:id id :tab-id tab-id}))]
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
        (e/form {:component "form" :isInvalid (boolean errors) :error errors}
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
            (e/file-picker {:onChange on-file-selected :accept "image/*"}))))
      (e/modal-footer {}
        (e/button {:onClick on-close} "Cancel")
        (e/button
          {:type      "submit"
           :fill      true
           :onClick   on-save
           :isLoading loading?
           :disabled  (not bm-valid?)
           :iconType  "save"
           :form      "tab-modal-form"}
          "Save")
        (e/button {:onClick on-clear} "Clear")))))

(def ui-bookmark-modal (comp/factory BookmarkModal {:keyfn :bookmark/id}))

(defsc Bookmark
  [this
   {:bookmark/keys [id title image favourite url tab-id]
    :ui/keys       [edit-mode?]}
   {:keys [on-click]}]
  {:ident :bookmark/id
   :query [:bookmark/id
           :bookmark/title
           :bookmark/url
           :bookmark/favourite
           :bookmark/image
           :bookmark/tab-id
           :ui/edit-mode?
           :ui/image]}
  (let [on-favourite #(comp/transact! this [(patch-bookmark #:bookmark{:id id :tab-id tab-id :favourite (not favourite)})])
        on-delete    #(comp/transact! this [(delete-bookmark #:bookmark{:id id :tab-id tab-id})])]
    (e/card
      {:title         title
       :titleElement  "h2"
       :onClick       (if (not edit-mode?) #(js/window.open url))
       :onContextMenu (fn [e]
                        (evt/prevent-default! e)
                        (js/console.log "Hello"))
       :image         (e/image
                        {:height "200vh"
                         :src    image})
       :footer        (if edit-mode?
                        (e/flex-group {:justifyContent "flexEnd"}
                          (e/flex-item {:grow false}
                            (e/button-icon
                              {:aria-label "favourite"
                               :size       "s"
                               :iconType   (if favourite "starFilled" "starEmpty")
                               :onClick    on-favourite}))
                          (e/flex-item {:grow false}
                            (e/button-icon
                              {:aria-label "edit"
                               :size       "s"
                               :iconType   "pencil"
                               :onClick    on-click}))
                          (e/flex-item {:grow false}
                            (e/button-icon
                              {:aria-label "delete"
                               :size       "s"
                               :iconType   "trash"
                               :onClick    on-delete
                               :color      "danger"}))))})))

(def ui-bookmark (comp/factory Bookmark {:keyfn :bookmark/id}))

(defsc NewBookmark
  [_ _ {:keys [on-click]}]
  {}
  (e/card
    {:title        "Add New"
     :titleElement "h2"
     :paddingSize  "l"
     :icon         (e/icon {:size "xxl" :type "plus"})
     :onClick      on-click}))

(def ui-new-bookmark (comp/factory NewBookmark))
