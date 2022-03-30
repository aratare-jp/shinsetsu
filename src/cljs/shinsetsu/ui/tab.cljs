(ns shinsetsu.ui.tab
  (:require
    [shinsetsu.application :refer [app]]
    [shinsetsu.ui.elastic :as e]
    [shinsetsu.mutations.tab :refer [create-tab patch-tab delete-tab]]
    [shinsetsu.mutations.bookmark :refer [fetch-bookmarks]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.mutations :as m]
    [shinsetsu.ui.bookmark :as bui]
    [com.fulcrologic.fulcro.dom :refer [div label input form button h1 h2 nav h5 p]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :refer [defrouter]]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [shinsetsu.schema :as s]
    [taoensso.timbre :as log]
    [malli.core :as mc]
    [shinsetsu.mutations.common :refer [remove-ident]]
    [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]))

(defsc TabModal
  [this {:tab/keys [id name is-protected?] :ui/keys [password loading? error-type change-password?]} {:keys [on-close]}]
  {:ident         :tab/id
   :query         [:tab/id :tab/name :tab/is-protected?
                   :ui/password :ui/loading? :ui/error-type :ui/change-password?
                   fs/form-config-join]
   :form-fields   #{:tab/name :ui/password :ui/change-password?}
   :initial-state (fn [_]
                    {:tab/id      (tempid/tempid)
                     :tab/name    ""
                     :ui/password ""})
   :pre-merge     (fn [{:keys [data-tree]}] (fs/add-form-config TabModal data-tree))}
  (let [new?       (tempid/tempid? id)
        on-blur    (fn [f] (comp/transact! this [(fs/mark-complete! {:field f})]))
        tab-valid? (mc/validate s/tab-form-spec #:tab{:name name :password password})
        on-close   (fn [_]
                     (comp/transact! this [(fs/reset-form! {:form-ident (comp/get-ident this)})])
                     (on-close))
        on-save    (fn []
                     (if new?
                       (comp/transact! this [(create-tab #:tab{:id id :name name :password password})])
                       (comp/transact! this [(patch-tab #:tab{:id id :name name :password password})])))
        on-clear   #(comp/transact! this [(fs/reset-form! {:form-ident (comp/get-ident this)})])
        errors     (case error-type
                     :invalid-input ["Unable to create new tab." "Please try again."]
                     :internal-server-error ["Unknown error encountered"]
                     nil)]
    (e/modal {:onClose on-close}
      (e/modal-header {}
        (e/modal-header-title {}
          (h1 (if (tempid/tempid? id) "Create New Tab" "Edit Tab"))))
      (e/modal-body {}
        (e/form {:component "form" :isInvalid (boolean errors) :error errors}
          (e/form-row {:label "Name"}
            (e/field-text
              {:name     "name"
               :value    name
               :onChange (fn [e] (m/set-string! this :tab/name :event e))
               :onBlur   #(on-blur :tab/name)
               :disabled loading?}))
          (if (not new?)
            (e/form-row {:label (if is-protected? "Change password?" "Lock this tab?")}
              (e/switch
                {:name     "change-password"
                 :checked  change-password?
                 :onChange (fn [] (m/set-value! this :ui/change-password? (not change-password?)))})))
          (if (or new? change-password?)
            (e/form-row {:label "Password" :helpText "Can be left empty if you don't want to lock this tab"}
              (e/field-text
                {:name     "password"
                 :value    password
                 :type     "password"
                 :onBlur   #(on-blur :ui/password)
                 :disabled loading?
                 :onChange (fn [e] (m/set-string! this :ui/password :event e))})))))
      (e/modal-footer {}
        (e/button {:onClick on-close} "Cancel")
        (e/button
          {:type      "submit"
           :fill      true
           :onClick   on-save
           :isLoading loading?
           :disabled  (not tab-valid?)
           :form      "tab-modal-form"}
          "Save")
        (e/button {:onClick on-clear} "Clear")))))

(def ui-tab-modal (comp/factory TabModal {:keyfn :tab/id}))

(defn- ui-error-prompt
  [this error-type]
  (let [back-fn #(m/set-value! this :ui/error-type nil)]
    (case error-type
      :wrong-password (e/empty-prompt {:color    "danger"
                                       :iconType "alert"
                                       :title    (h2 "Wrong Password!")
                                       :body     (p "Please try again")
                                       :actions  [(e/button {:fill true :onClick back-fn} "Back")]})
      :internal-server-error (e/empty-prompt {:color    "danger"
                                              :iconType "alert"
                                              :title    (h2 "Internal Server Error")
                                              :body     (p "Please try again")
                                              :actions  [(e/button {:fill true :onClick back-fn} "Back")]})
      (e/empty-prompt {:color    "danger"
                       :iconType "alert"
                       :title    (h2 "I have no idea what's going on either!")
                       :body     (p "Please try again")
                       :actions  [(e/button {:fill true :onClick back-fn} "Back")]}))))

(defn- ui-bookmark-modal
  [this id selected-bm-idx bookmarks]
  (if selected-bm-idx
    (let [bookmark (merge (nth bookmarks selected-bm-idx) {:bookmark/tab-id id})
          on-close (fn []
                     (m/set-value! this :ui/show-bookmark-modal? false)
                     (m/set-value! this :ui/selected-bm-idx nil))]
      (bui/ui-bookmark-modal (comp/computed bookmark {:on-close on-close})))
    (let [new-bookmark (first (filter (fn [b] (tempid/tempid? (:bookmark/id b))) bookmarks))
          bm-ident     (comp/get-ident bui/BookmarkModal new-bookmark)
          on-close     (fn []
                         (m/set-value! this :ui/show-bookmark-modal? false)
                         (comp/transact! this [(remove-ident {:ident bm-ident})]))]
      (bui/ui-bookmark-modal (comp/computed new-bookmark {:on-close on-close})))))

(defn- ui-tab-body
  [this id name bookmarks]
  (let [add-bm-btn     (e/button
                         {:fill     true
                          :iconType "plus"
                          :onClick  (fn []
                                      (merge/merge-component!
                                        app bui/BookmarkModal
                                        #:bookmark{:id (tempid/tempid) :title "" :url "" :tab-id id}
                                        :append [:tab/id id :tab/bookmarks])
                                      (m/set-value! this :ui/show-bookmark-modal? true))}
                         "Add Bookmark")
        delete-tab-btn (e/button
                         {:iconType "trash"
                          :color    "danger"
                          :onClick  #(comp/transact! this [(delete-tab {:tab/id id})])}
                         "Delete")
        edit-tab-btn   (e/button
                         {:iconType "pencil"
                          :onClick  #(m/set-value! this :ui/show-tab-modal? true)}
                         "Edit")]
    (if (empty? bookmarks)
      (e/empty-prompt {:title   (p "Seems like you don't have any bookmark")
                       :body    (p "Let's add your first bookmark!")
                       :actions [add-bm-btn edit-tab-btn delete-tab-btn]})
      (e/page-template
        {:pageHeader {:pageTitle      (h2 name)
                      :rightSideItems [add-bm-btn edit-tab-btn delete-tab-btn]}}
        (e/flex-grid {:columns 3}
          (map-indexed
            (fn [i {bookmark-id :bookmark/id :as bookmark}]
              (let [on-click (fn []
                               (m/set-integer! this :ui/selected-bm-idx :value i)
                               (m/set-value! this :ui/show-bookmark-modal? true))
                    bookmark (merge bookmark {:bookmark/tab-id id})]
                (e/flex-item {:key bookmark-id}
                  (bui/ui-bookmark (comp/computed bookmark {:on-click on-click})))))
            bookmarks))))))

(defn- ui-unlock-prompt
  [this id name bookmarks unlocked? password]
  (if unlocked?
    (ui-tab-body this id name bookmarks)
    (e/empty-prompt {:title   (h2 "This tab is protected!")
                     :body    (e/form {:component "form" :id "tab-password-modal"}
                                (e/form-row {:label "Password"}
                                  (e/field-text {:type     "password"
                                                 :name     "password"
                                                 :value    password
                                                 :onChange #(m/set-string! this :ui/password :event %)})))
                     :actions [(e/button
                                 {:fill    true
                                  :onClick #(comp/transact! this [(fetch-bookmarks #:tab{:id id :password password})])}
                                 "Unlock this tab!")]})))

(defsc Tab
  [this
   {:tab/keys [id name is-protected? bookmarks]
    :ui/keys  [password unlocked? error-type show-bookmark-modal? show-tab-modal? selected-bm-idx]
    :as       props}]
  {:ident             :tab/id
   :query             [:tab/id
                       :tab/name
                       :tab/is-protected?
                       {:tab/bookmarks (comp/get-query bui/BookmarkModal)}
                       :ui/unlocked?
                       :ui/password
                       :ui/change-password?
                       :ui/error-type
                       :ui/show-bookmark-modal?
                       :ui/show-tab-modal?
                       :ui/selected-bm-idx]
   :componentDidMount (fn [this]
                        (let [{:tab/keys [id is-protected?]} (comp/props this)]
                          (if (not is-protected?)
                            (comp/transact! this [(fetch-bookmarks #:tab{:id id})]))))}

  [(if show-bookmark-modal?
     (ui-bookmark-modal this id selected-bm-idx bookmarks))
   (if show-tab-modal?
     (let [on-close #(m/set-value! this :ui/show-tab-modal? false)]
       (ui-tab-modal (comp/computed props {:on-close on-close}))))
   (let [bookmarks (filter #(not (tempid/tempid? (:bookmark/id %))) bookmarks)]
     (cond
       error-type (ui-error-prompt this error-type)
       is-protected? (ui-unlock-prompt this id name bookmarks unlocked? password)
       :else (ui-tab-body this id name bookmarks)))])

(def ui-tab (comp/factory Tab {:keyfn :tab/id}))
