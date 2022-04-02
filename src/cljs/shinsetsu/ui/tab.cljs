(ns shinsetsu.ui.tab
  (:require
    [shinsetsu.application :refer [app]]
    [shinsetsu.ui.elastic :as e]
    [shinsetsu.mutations.tab :refer [create-tab patch-tab delete-tab lock-tab]]
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
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.dom.events :as evt]))

(defn- ui-edit-switch
  [this edit-mode?]
  (e/switch {:label "Edit" :checked edit-mode? :onChange #(m/toggle! this :ui/edit-mode?)}))

(defsc TabModal
  [this
   {:tab/keys [id name is-protected?]
    :ui/keys  [password loading? error-type change-password?]}
   {:keys [on-close]}]
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
        on-save    (fn [e]
                     (evt/prevent-default! e)
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
        (e/form {:id "tab-modal-form" :component "form" :isInvalid (boolean errors) :error errors}
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
  [this id bookmarks edit-mode?]
  (let [add-bm-fn  (fn []
                     (merge/merge-component!
                       app bui/BookmarkModal
                       (comp/get-initial-state bui/BookmarkModal {:bookmark/tab-id id})
                       :append [:tab/id id :tab/bookmarks])
                     (m/set-value! this :ui/show-bookmark-modal? true))
        add-bm-btn (e/button {:fill true :iconType "plus" :onClick add-bm-fn} "Add Bookmark")]
    (if (empty? bookmarks)
      (e/empty-prompt {:title   (p "Seems like you don't have any bookmark")
                       :body    (p "Let's add your first bookmark!")
                       :actions [add-bm-btn]})
      (div
        (ui-edit-switch this edit-mode?)
        (e/spacer)
        (e/flex-grid {:columns 3}
          (e/spacer {})
          (map-indexed
            (fn [i {bookmark-id :bookmark/id :as bookmark}]
              (let [on-click (fn []
                               (m/set-integer! this :ui/selected-bm-idx :value i)
                               (m/set-value! this :ui/show-bookmark-modal? true))
                    bookmark (merge bookmark {:bookmark/tab-id id :ui/edit-mode? edit-mode?})]
                (e/flex-item {:key bookmark-id}
                  (bui/ui-bookmark (comp/computed bookmark {:on-click on-click})))))
            bookmarks)
          (e/flex-item {:key "new-bookmark"}
            (bui/ui-new-bookmark (comp/computed {} {:on-click add-bm-fn}))))))))

(defn- ui-unlock-prompt
  [this id bookmarks unlocked? password edit-mode?]
  (if unlocked?
    (ui-tab-body this id bookmarks edit-mode?)
    (e/empty-prompt
      {:color   "plain"
       :title   (h2 "This tab is protected!")
       :body    (e/form {:component "form" :id "tab-password-modal"}
                  (e/form-row {:label "Password"}
                    (e/field-password {:type     "dual"
                                       :name     "password"
                                       :value    password
                                       :onChange #(m/set-string! this :ui/password :event %)})))
       :actions [(e/button
                   {:fill    true
                    :onClick #(comp/transact! this [{(fetch-bookmarks #:tab{:id id :password password})
                                                     [:tab/id {:tab/bookmarks (comp/get-query bui/Bookmark)}]}])}
                   "Unlock this tab!")]})))

(defsc Tab
  [this
   {:tab/keys [id is-protected? bookmarks]
    :ui/keys  [password unlocked? error-type show-bookmark-modal? selected-bm-idx edit-mode?]}]
  {:ident              :tab/id
   :query              [:tab/id
                        :tab/name
                        :tab/is-protected?
                        {:tab/bookmarks (comp/get-query bui/BookmarkModal)}
                        :ui/unlocked?
                        :ui/password
                        :ui/change-password?
                        :ui/error-type
                        :ui/show-bookmark-modal?
                        :ui/edit-mode?
                        :ui/selected-bm-idx]
   :componentWillMount (fn [this]
                         (log/info "Loading user bookmarks")
                         (let [{:tab/keys [id is-protected?]} (comp/props this)]
                           (if (not is-protected?)
                             (comp/transact! this [{(fetch-bookmarks #:tab{:id id})
                                                    [:tab/id {:tab/bookmarks (comp/get-query bui/Bookmark)}]}]))))}

  [(e/spacer {})
   (if show-bookmark-modal?
     (ui-bookmark-modal this id selected-bm-idx bookmarks))
   (let [bookmarks (filter #(not (tempid/tempid? (:bookmark/id %))) bookmarks)]
     (cond
       error-type (ui-error-prompt this error-type)
       is-protected? (ui-unlock-prompt this id bookmarks unlocked? password edit-mode?)
       :else (ui-tab-body this id bookmarks edit-mode?)))])

(def ui-tab (comp/factory Tab {:keyfn :tab/id}))

(defn- show-if-unlocked
  ([tabs selected-tab-idx btn] (show-if-unlocked tabs selected-tab-idx btn true))
  ([tabs selected-tab-idx btn show-if-public?]
   (if-let [curr-tab (and (not (empty? tabs)) (nth tabs selected-tab-idx))]
     (let [is-protected? (:tab/is-protected? curr-tab)
           unlocked?     (:ui/unlocked? curr-tab)]
       (if is-protected?
         (if unlocked?
           btn)
         (if show-if-public?
           btn))))))

(defn- ui-tab-headers
  [this tabs selected-tab-idx]
  (map-indexed
    (fn [i {:tab/keys [id name is-protected?] :ui/keys [unlocked?]}]
      (e/tab
        {:id            id
         :prepend       (if is-protected?
                          (if unlocked?
                            (e/icon {:type "lockOpen"})
                            (e/icon {:type "lock"})))
         :onContextMenu (fn [e]
                          (evt/prevent-default! e)
                          (js/console.log "Hello"))
         :onClick       #(m/set-integer! this :ui/selected-tab-idx :value i)
         :isSelected    (= i selected-tab-idx)}
        name))
    tabs))

(defn- ui-new-tab
  [this tabs]
  (let [new-tab  (first (filter #(tempid/tempid? (:tab/id %)) tabs))
        on-close (fn []
                   (comp/transact! this [(remove-ident {:ident (comp/get-ident TabModal new-tab)})])
                   (m/set-value! this :ui/show-tab-modal? false))]
    (ui-tab-modal (comp/computed new-tab {:on-close on-close}))))

(defn ui-edit-tab
  [this tab]
  (let [on-close #(m/set-value! this :ui/show-edit-modal? false)]
    (ui-tab-modal (comp/computed tab {:on-close on-close}))))

(defn- ui-delete-tab
  [this {:tab/keys [id name]}]
  (e/confirm-modal
    {:title             (str "Delete tab " name)
     :onCancel          #(m/set-value! this :ui/show-delete-modal? false)
     :onConfirm         #(comp/transact! this [(delete-tab {:tab/id id})])
     :cancelButtonText  "Cancel"
     :confirmButtonText "Yes, I'm sure!"
     :buttonColor       "danger"}
    (p "Deleting this tab will also delete all the bookmarks within it!")
    (p "Are you sure you want to delete this tab?")))

(defn- ui-tab-main-body
  [this tabs selected-tab-idx]
  (let [right-side-items (->> [(e/button-icon
                                 {:fill     true
                                  :size     "m"
                                  :onClick  (fn []
                                              (m/set-value! this :ui/show-tab-modal? true)
                                              (merge/merge-component!
                                                app TabModal (comp/get-initial-state TabModal)
                                                :append [:root/tabs]))
                                  :iconType "plus"})
                               (show-if-unlocked
                                 tabs
                                 selected-tab-idx
                                 (e/button-icon
                                   {:fill     true
                                    :size     "m"
                                    :onClick  #(m/set-value! this :ui/show-edit-modal? true)
                                    :iconType "pencil"}))
                               (show-if-unlocked
                                 tabs
                                 selected-tab-idx
                                 (e/button-icon
                                   {:fill     true
                                    :size     "m"
                                    :color    "danger"
                                    :onClick  #(m/set-value! this :ui/show-delete-modal? true)
                                    :iconType "trash"}))
                               (show-if-unlocked
                                 tabs
                                 selected-tab-idx
                                 (e/button-icon
                                   {:fill     true
                                    :size     "m"
                                    :iconType "lock"
                                    :onClick  #(if-let [curr-tab (nth tabs selected-tab-idx)]
                                                 (comp/transact! this [(lock-tab curr-tab)]))})
                                 false)]
                              (filter #(not (nil? %))))]
    (e/page-template
      {:pageHeader {:pageTitle      (if (empty? tabs)
                                      "Welcome!"
                                      (-> tabs (nth selected-tab-idx) :tab/name))
                    :rightSideItems right-side-items}}
      (e/tabs {:size "xl"}
        (ui-tab-headers this tabs selected-tab-idx))
      (if (empty? tabs)
        (e/empty-prompt {:title (h2 "It seems like you don't have any tab at the moment.")
                         :body  (p "Start enjoying Shinsetsu by add or import your bookmarks")})
        (ui-tab (nth tabs selected-tab-idx))))))

(defsc TabMain
  [this {:root/keys [tabs] :ui/keys [selected-tab-idx show-tab-modal? show-edit-modal? show-delete-modal?]}]
  {:ident         (fn [] [:component/id ::tab])
   :route-segment ["tab"]
   :query         [{[:root/tabs '_] (comp/get-query Tab)}
                   :ui/selected-tab-idx
                   :ui/show-edit-modal?
                   :ui/show-delete-modal?
                   :ui/show-tab-modal?]
   :initial-state {:root/tabs             []
                   :ui/selected-tab-idx   0
                   :ui/show-tab-modal?    false
                   :ui/show-delete-modal? false
                   :ui/show-edit-modal?   false}
   :will-enter    (fn [app _]
                    (log/info "Loading user tabs")
                    (dr/route-deferred
                      [:component/id ::tab]
                      #(let [load-target (targeting/replace-at [:root/tabs])]
                         ;; FIXME: Needs to load from local storage first before fetching from remote.
                         (df/load! app :user/tabs Tab {:target               load-target
                                                       :post-mutation        `dr/target-ready
                                                       :post-mutation-params {:target [:component/id ::tab]}}))))}
  [(if show-tab-modal?
     (ui-new-tab this tabs))
   (if show-edit-modal?
     (ui-edit-tab this (nth tabs selected-tab-idx)))
   (if show-delete-modal?
     (ui-delete-tab this (nth tabs selected-tab-idx)))
   (as-> tabs $
         (filter #(not (tempid/tempid? (:tab/id %))) $)
         (ui-tab-main-body this $ selected-tab-idx))])
