(ns shinsetsu.ui.tag
  (:require
    [shinsetsu.ui.elastic :as e]
    [shinsetsu.application :refer [app]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :refer [div label input form button h1 h2 nav h5 p span]]
    [com.fulcrologic.fulcro.mutations :as m]
    [shinsetsu.mutations.common :refer [remove-ident]]
    [shinsetsu.mutations.tag :refer [create-tag patch-tag delete-tag fetch-tags]]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [malli.core :as mc]
    [shinsetsu.schema :as s]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]))

(defsc TagModal
  [this
   {:tag/keys [id name colour]
    :ui/keys  [loading? error-type]}
   {:keys [on-close]}]
  {:ident         :tag/id
   :query         [:tag/id :tag/name :tag/colour
                   :ui/loading? :ui/error-type :ui/change-password?
                   fs/form-config-join]
   :form-fields   #{:tag/name :tag/colour}
   :initial-state (fn [_]
                    {:tag/id     (tempid/tempid)
                     :tag/name   ""
                     :tag/colour "#ffffff"})
   :pre-merge     (fn [{:keys [data-tree]}] (fs/add-form-config TagModal data-tree))}
  (let [new?       (tempid/tempid? id)
        on-blur    (fn [f] (comp/transact! this [(fs/mark-complete! {:field f})]))
        tag-valid? (mc/validate s/tag-form-spec #:tag{:name name :tag/colour colour})
        on-close   (fn [_]
                     (comp/transact! this [(fs/reset-form! {:form-ident (comp/get-ident this)})])
                     (on-close))
        on-save    (fn [e]
                     (evt/prevent-default! e)
                     (if new?
                       (comp/transact! this [(create-tag #:tag{:id id :name name :colour colour})])
                       (comp/transact! this [(patch-tag #:tag{:id id :name name :colour colour})])))
        on-clear   #(comp/transact! this [(fs/reset-form! {:form-ident (comp/get-ident this)})])
        errors     (case error-type
                     :invalid-input ["Unable to create new tag." "Please try again."]
                     :internal-server-error ["Unknown error encountered"]
                     nil)]
    (e/modal {:onClose on-close}
      (e/modal-header {}
        (e/modal-header-title {}
          (h1 (if (tempid/tempid? id) "Create New Tag" "Edit Tag"))))
      (e/modal-body {}
        (e/form {:id "tag-modal-form" :component "form" :isInvalid (boolean errors) :error errors}
          (e/form-row {:label "Name"}
            (e/field-text
              {:name     "name"
               :value    name
               :onChange #(m/set-string! this :tag/name :event %)
               :onBlur   #(on-blur :tag/name)
               :disabled loading?}))
          (e/form-row {:label "Colour"}
            (e/colour-picker
              {:color    colour
               :onChange #(m/set-value! this :tag/colour %)
               :disabled loading?}))))
      (e/modal-footer {}
        (e/button
          {:type      "submit"
           :fill      true
           :iconType  "save"
           :onClick   on-save
           :isLoading loading?
           :disabled  (not tag-valid?)
           :form      "tag-modal-form"}
          "Save")
        (e/button {:onClick on-clear} "Clear")))))

(def ui-tag-modal (comp/factory TagModal {:keyfn :tag/id}))

(defn- ui-new-tag
  [this tags]
  (let [new-tag  (first (filter #(tempid/tempid? (:tag/id %)) tags))
        on-close (fn []
                   (comp/transact! this [(remove-ident {:ident (comp/get-ident TagModal new-tag)})])
                   (m/set-value! this :ui/show-create-modal? false))]
    (ui-tag-modal (comp/computed new-tag {:on-close on-close}))))

(defn- ui-edit-tag
  [this tag]
  (let [on-close #(m/set-value! this :ui/edit-tag-id nil)]
    (ui-tag-modal (comp/computed tag {:on-close on-close}))))

(defn- ui-delete-tag
  [this {:tag/keys [id name] :as tag}]
  (e/confirm-modal
    {:title             (str "Delete tag " name)
     :onCancel          #(m/set-value! this :ui/show-delete-modal? false)
     :onConfirm         #(comp/transact! this [(delete-tag {:tag/id id})])
     :cancelButtonText  "Cancel"
     :confirmButtonText "Yes, I'm sure!"
     :buttonColor       "danger"}
    (p "Deleting this tag will also remove all assignments!")
    (p "Are you sure you want to delete this tag?")))

(defn- ui-tag-main-body
  [this tags]
  (e/page-template
    {:pageHeader {:pageTitle      "Tags"
                  :rightSideItems [(e/button
                                     {:fill       true
                                      :iconType   "plus"
                                      :size       "l"
                                      :aria-label "add-tag"
                                      :onClick    (fn []
                                                    (m/set-value! this :ui/show-create-modal? true)
                                                    (merge/merge-component!
                                                      app TagModal (comp/get-initial-state TagModal)
                                                      :append (conj (comp/get-ident this) :ui/tags)))}
                                     "Create new tag")]}}
    (if (empty? tags)
      (e/empty-prompt {:title (h2 "It seems like you don't have any tag at the moment.")
                       :body  (p "Start enjoying Shinsetsu by add or import your tags")})
      (e/list-group {:bordered true :size "l"}
        (map-indexed
          (fn [i {:tag/keys [id name colour]}]
            (e/list-group-item
              {:label       (e/badge {:color colour} name)
               :size        "l"
               :onClick     #(m/set-value! this :ui/edit-tag-id id)
               :extraAction {:iconType "cross"
                             :iconSize "m"
                             :onClick  (fn []
                                         (m/set-value! this :ui/edit-tag-id id)
                                         (m/toggle! this :ui/show-delete-modal?))}}))
          tags)))))

(defsc TagMain
  [this
   {:ui/keys [tags current-tag-idx show-create-modal? edit-tag-id show-delete-modal?]}]
  {:ident             (fn [] [:component/id ::tag])
   :route-segment     ["tag"]
   :query             [:ui/current-tag-idx
                       :ui/show-create-modal?
                       :ui/edit-tag-id
                       :ui/show-delete-modal?
                       {:ui/tags (comp/get-query TagModal)}]
   :initial-state     {:ui/tags               []
                       :ui/current-tag-idx    0
                       :ui/show-create-modal? false
                       :ui/show-delete-modal? false}
   :componentDidMount (fn [this]
                        (log/info "Loading user tags")
                        (m/set-value! this :ui/loading? true)
                        (df/load! this :user/tags TagModal {:target   (targeting/replace-at [:component/id ::tag :ui/tags])
                                                            :fallback `shinsetsu.mutations.tag/post-tags-error-load}))}
  [(if show-create-modal?
     (ui-new-tag this tags))
   (if edit-tag-id
     (ui-edit-tag this (nth tags current-tag-idx)))
   (if show-delete-modal?
     (ui-delete-tag this (nth tags current-tag-idx)))
   (->> tags
        (filter #(not (tempid/tempid? (:tag/id %))))
        (ui-tag-main-body this))])
