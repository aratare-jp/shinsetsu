(ns shinsetsu.ui.tab
  (:require
    [clojure.browser.dom :refer [get-element]]
    [com.fulcrologic.fulcro.algorithms.data-targeting]
    [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.dom :refer [button div form h1 h2 h5 input label nav p span]]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
    [goog.functions :as gf]
    [malli.core :as mc]
    [shinsetsu.application :refer [app]]
    [shinsetsu.mutations.common :refer [remove-ident set-root]]
    [shinsetsu.mutations.tab :as tab-mut]
    [shinsetsu.schema :as s]
    [shinsetsu.ui.bookmark :as bui]
    [shinsetsu.ui.elastic :as e]
    [taoensso.timbre :as log]))

(defsc TabModal
  [this
   {:tab/keys [id name]
    :ui/keys  [password loading? error-type change-password?]}
   {:keys [on-close]}]
  {:ident         :tab/id
   :query         [:tab/id :tab/name :ui/password :ui/loading? :ui/error-type :ui/change-password?
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
              {:name        "name"
               :value       name
               :placeholder "Add name here"
               :disabled    loading?
               :onBlur      #(on-blur :tab/name)
               :onChange    #(m/set-string! this :tab/name :event %)}))
          (e/form-row {:label (if new? "Add password?" "Change password?")}
            (e/switch
              {:name     "change-password"
               :checked  change-password?
               :onChange #(m/toggle! this :ui/change-password?)}))
          (if change-password?
            (e/form-row
              {:label    "Password"
               :helpText (if (not new?) "Can be left empty if you don't want to lock this tab")}
              (e/field-text
                {:name        "password"
                 :value       password
                 :placeholder "Add password here"
                 :type        "password"
                 :disabled    loading?
                 :onBlur      #(on-blur :ui/password)
                 :onChange    (fn [e] (m/set-string! this :ui/password :event e))})))))
      (e/modal-footer {}
        (e/button {:onClick on-close} "Cancel")
        (e/button
          {:type      "submit"
           :fill      true
           :isLoading loading?
           :disabled  (not tab-valid?)
           :form      "tab-modal-form"
           :onClick   (fn [e]
                        (evt/prevent-default! e)
                        (if new?
                          (comp/transact! this [(tab-mut/create-tab #:tab{:id id :name name :password password})])
                          (if change-password?
                            (comp/transact! this [(tab-mut/patch-tab #:tab{:id id :name name :password password :change-password? true})])
                            (comp/transact! this [(tab-mut/patch-tab #:tab{:id id :name name})]))))}
          "Save")
        (e/button
          {:onClick #(comp/transact! this [(fs/reset-form! {:form-ident (comp/get-ident this)})])}
          "Clear")))))

(def ui-tab-modal (comp/factory TabModal {:keyfn :tab/id}))

(defn- ui-loading-tab
  []
  (e/flex-grid {:columns 3}
    (e/flex-item {}
      (bui/ui-loading-bookmark))
    (e/flex-item {}
      (bui/ui-loading-bookmark))
    (e/flex-item {}
      (bui/ui-loading-bookmark))))

(defn- ui-tab-body
  [this {:tab/keys [id bookmarks] :ui/keys [loading?]}]
  (let [add-bm-fn  #(let [new-bookmark (comp/get-initial-state bui/BookmarkModal {:bookmark/tab-id id})]
                      (merge/merge-component! app bui/BookmarkModal new-bookmark :append [:tab/id id :tab/bookmarks])
                      (m/set-value! this :ui/edit-bm-id (:bookmark/id new-bookmark)))
        add-bm-btn (e/button {:fill true :iconType "plus" :onClick add-bm-fn} "Add Bookmark")]
    (cond
      (and loading? (empty? bookmarks))
      (ui-loading-tab)
      (empty? bookmarks)
      (e/empty-prompt {:title   (p "Seems like you don't have any bookmark")
                       :body    (p "Let's add your first bookmark!")
                       :actions [add-bm-btn]})
      :else
      [(if loading?
         [(e/progress {:size "xs"})
          (e/spacer)])
       (e/flex-grid {:columns 3}
         (->> bookmarks
              (filter (fn [b] (not (tempid/tempid? (:bookmark/id b)))))
              (map
                (fn [{bookmark-id :bookmark/id :as bookmark}]
                  (let [on-edit   #(m/set-value! this :ui/edit-bm-id bookmark-id)
                        on-delete #(m/set-value! this :ui/delete-bm-id bookmark-id)
                        on-move   #(m/set-value! this :ui/move-bm-id %)
                        bookmark  (-> bookmark
                                      (assoc :bookmark/tab-id id)
                                      (comp/computed {:on-edit on-edit :on-delete on-delete :on-move on-move}))]
                    (e/flex-item
                      {:key bookmark-id}
                      (bui/ui-bookmark bookmark))))))
         (e/flex-item {:key "new-bookmark"}
           (bui/ui-new-bookmark (comp/computed {} {:on-edit add-bm-fn}))))])))

(defn- ui-locked-tab-body
  [this {:tab/keys [id] :ui/keys [loading? unlocked? password error-type search-query sort-option] :as props}]
  (if-not unlocked?
    (let [form-id (str "tab-" id "-unlock-form")
          errors  (case error-type
                    :wrong-password ["Invalid password"]
                    :internal-server-error ["Internal server error"]
                    nil)]
      (e/empty-prompt
        {:color   "transparent"
         :title   (h2 "This tab is protected!")
         :body    (e/form {:id form-id :component "form" :isInvalid (boolean errors) :error errors}
                    (e/form-row {:label "Password"}
                      (e/field-password {:type     "dual"
                                         :name     "password"
                                         :value    password
                                         :disabled loading?
                                         :onChange #(m/set-string! this :ui/password :event %)})))
         :actions [(e/button
                     {:fill      true
                      :type      "submit"
                      :form      form-id
                      :disabled  loading?
                      :isLoading loading?
                      :onClick   (fn [e]
                                   (evt/prevent-default! e)
                                   (log/info "Load user bookmarks")
                                   (m/set-value! this :ui/loading? true)
                                   (df/load-field! this :tab/bookmarks {:params               {:password password
                                                                                               :query    search-query
                                                                                               :sort     sort-option}
                                                                        :post-mutation        `tab-mut/post-load-locked-bookmarks
                                                                        :post-mutation-params {:tab/id id}
                                                                        :fallback             `tab-mut/post-load-bookmarks-error}))}
                     "Unlock this tab!")]}))
    (ui-tab-body this props)))

(defsc Tab
  [this {:tab/keys [is-protected?] :ui/keys [edit-bm-id delete-bm-id move-bm-id] :as props}]
  {:ident                :tab/id
   :query                [:tab/id
                          :tab/name
                          :tab/is-protected?
                          {:tab/bookmarks (comp/get-query bui/BookmarkModal)}
                          {[:ui/tabs '_] [:tab/id :tab/name]}
                          [:ui/search-query '_]
                          [:ui/sort-option '_]
                          :ui/unlocked?
                          :ui/password
                          :ui/change-password?
                          :ui/error-type
                          :ui/edit-bm-id
                          :ui/current-tab-idx
                          :ui/loading?
                          :ui/load-timer
                          :ui/delete-bm-id
                          :ui/move-bm-id
                          :ui/destination-tab-id]
   :componentWillMount   (fn [this]
                           (let [{:tab/keys [id is-protected?] :ui/keys [unlocked? search-query sort-option]} (comp/props this)]
                             (if (or (not is-protected?) (and is-protected? unlocked?))
                               (do
                                 (log/info "Load user bookmarks")
                                 (m/set-value! this :ui/loading? true)
                                 (df/load-field! this :tab/bookmarks {:without              #{:bookmark/tab-id}
                                                                      :params               {:query search-query
                                                                                             :sort  sort-option}
                                                                      :post-mutation        `tab-mut/post-load-unlocked-bookmarks
                                                                      :post-mutation-params {:tab/id id}
                                                                      :fallback             `tab-mut/post-load-bookmarks-error})))))
   :componentWillUnmount (fn [this]
                           ;; TODO: Lock tab when moving away. Not sure about how annoying this can be for users.
                           (m/set-value! this :ui/password nil)
                           (m/set-value! this :ui/unlocked? false))
   :componentDidUpdate   (fn [this prev-props _]
                           (let [{:tab/keys [id is-protected?] :ui/keys [unlocked? search-query sort-option]} (comp/props this)
                                 prev-query       (:ui/search-query prev-props)
                                 prev-sort-option (:ui/sort-option prev-props)]
                             ;; Re-fetch if either this is a normal tab, or it's an unlocked protected tab.
                             (if (and (or (not is-protected?) (and is-protected? unlocked?))
                                      (or (not= search-query prev-query) (not= sort-option prev-sort-option)))
                               (do
                                 (m/set-value! this :ui/loading? true)
                                 (df/load-field! this :tab/bookmarks {:without              #{:bookmark/tab-id}
                                                                      :params               {:query search-query
                                                                                             :sort  sort-option}
                                                                      :post-mutation        `tab-mut/post-load-unlocked-bookmarks
                                                                      :post-mutation-params {:tab/id id}
                                                                      :fallback             `tab-mut/post-load-bookmarks-error})))))}
  [(if edit-bm-id
     (if (tempid/tempid? edit-bm-id)
       (bui/ui-new-bookmark-modal this props)
       (bui/ui-edit-bookmark-modal this props)))
   (if delete-bm-id
     (bui/ui-delete-bookmark-modal this props))
   (if move-bm-id
     (bui/ui-move-bookmark-modal this props))
   (cond
     is-protected? (ui-locked-tab-body this props)
     :else (ui-tab-body this props))])

(def ui-tab (comp/factory Tab {:keyfn :tab/id}))

(defn- ui-new-tab-modal
  [this {:ui/keys [tabs]}]
  (let [new-tab  (first (filter #(tempid/tempid? (:tab/id %)) tabs))
        on-close (fn []
                   (comp/transact! this [(remove-ident {:ident (comp/get-ident TabModal new-tab)})])
                   (m/set-value! this :ui/edit-tab-id nil))]
    (ui-tab-modal (comp/computed new-tab {:on-close on-close}))))

(defn- ui-edit-tab-modal
  [this {:ui/keys [tabs edit-tab-id]}]
  (let [tab (->> tabs (filter #(= edit-tab-id (:tab/id %))) first)]
    (let [on-close #(m/set-value! this :ui/edit-tab-id nil)]
      (ui-tab-modal (comp/computed tab {:on-close on-close})))))

(defn- ui-delete-tab-modal
  [this {:ui/keys [tabs delete-tab-id]}]
  (let [{:tab/keys [id name]} (->> tabs (filter #(= delete-tab-id (:tab/id %))) first)]
    (e/confirm-modal
      {:title             (str "Delete tab " name)
       :onCancel          #(m/set-value! this :ui/delete-tab-id nil)
       :onConfirm         #(comp/transact! this [(tab-mut/delete-tab {:tab/id id})])
       :cancelButtonText  "Cancel"
       :confirmButtonText "Yes, I'm sure!"
       :buttonColor       "danger"}
      (p "Deleting this tab will also delete all the bookmarks within it!")
      (p "Are you sure you want to delete this tab?"))))

(defn- ui-tab-headers
  [this {:ui/keys [tabs current-tab-idx tab-ctx-menu-id]}]
  (let [close-ctx-menu-fn #(m/set-value! this :ui/tab-ctx-menu-id nil)]
    (map-indexed
      (fn [i {:tab/keys [id name is-protected?] :ui/keys [unlocked?] :as tab}]
        [(if (= id tab-ctx-menu-id)
           (let [el (get-element (str "tab-" id))]
             (e/wrapping-popover
               {:button           el
                :initialFocus     false
                :anchorPosition   "upCenter"
                :panelPaddingSize "s"
                :isOpen           (= id tab-ctx-menu-id)
                :closePopover     close-ctx-menu-fn}
               (if (or (not is-protected?) unlocked?)
                 (e/flex-group {:gutterSize "none" :justifyContent "spaceAround"}
                   (if is-protected?
                     (e/flex-item {:grow false}
                       (e/button-icon
                         {:aria-label "lock"
                          :size       "s"
                          :iconType   "lock"
                          :onClick    (fn []
                                        (comp/transact! this [(tab-mut/lock-tab tab)])
                                        (close-ctx-menu-fn))})))
                   (e/flex-item {:grow false}
                     (e/button-icon
                       {:aria-label "edit"
                        :size       "s"
                        :iconType   "pencil"
                        :onClick    (fn []
                                      (m/set-value! this :ui/edit-tab-id id)
                                      (close-ctx-menu-fn))}))
                   (e/flex-item {:grow false}
                     (e/button-icon
                       {:aria-label "delete"
                        :size       "s"
                        :iconType   "trash"
                        :onClick    (fn []
                                      (m/set-value! this :ui/delete-tab-id id)
                                      (close-ctx-menu-fn))
                        :color      "danger"})))
                 (e/text {} "This tab is currently locked!")))))
         (e/tab
           {:prepend       (if is-protected?
                             (if unlocked?
                               (e/icon {:type "lockOpen"})
                               (e/icon {:type "lock"})))
            :onContextMenu (fn [e]
                             (evt/prevent-default! e)
                             (m/set-value! this :ui/tab-ctx-menu-id (:tab/id tab)))
            :onClick       #(m/set-integer! this :ui/current-tab-idx :value i)
            :isSelected    (= i current-tab-idx)}
           (div
             (div {:id (str "tab-" id)})
             (p name)))])
      tabs)))

(defn- ui-search-bar
  [this {:ui/keys [search-error-type loading?]}]
  (let [schema {:strict true? :fields {:tag {:type "string"} :name {:type "string"}}}]
    (e/input-popover
      {:isOpen search-error-type
       :input  (e/search-bar
                 {:box      {:schema schema :incremental true :isLoading loading?}
                  :onChange (gf/debounce
                              (fn [q]
                                (let [{:keys [query error]} (js->clj q :keywordize-keys true)]
                                  (if error
                                    (comp/transact! this [(set-root {:ui/search-error-type error})])
                                    (comp/transact! this [(set-root {:ui/search-query      (e/query->EsQuery query)
                                                                     :ui/search-error-type nil})]))))
                              500)})}
      "Your query seems to be incorrect")))

(defn- ui-tab-main-body
  [this {:ui/keys [tabs current-tab-idx search-query] :as props}]
  (let [tabs (filter #(not (tempid/tempid? (:tab/id %))) tabs)]
    (e/page-template {:pageHeader {:pageTitle      (if (empty? tabs)
                                                     "Welcome!"
                                                     (e/description-list {}
                                                       (e/description-list-title {}
                                                         (e/title {:size "l"}
                                                           (h1 (:tab/name (nth tabs current-tab-idx)))))
                                                       (e/description-list-description {}
                                                         (e/title {:size "xs"}
                                                           (h2 "1,000 items")))))
                                   :rightSideItems [(e/button
                                                      {:fill     true
                                                       :size     "m"
                                                       :iconType "plus"
                                                       :onClick  #(let [new-tab (comp/get-initial-state TabModal)]
                                                                    (merge/merge-component!
                                                                      app TabModal new-tab
                                                                      :append [:ui/tabs])
                                                                    (m/set-value! this :ui/edit-tab-id (:tab/id new-tab)))}
                                                      "New Tab")]}}
      (e/tabs {:size "xl"}
        (ui-tab-headers this props))
      (e/spacer {})
      (if (empty? tabs)
        (e/empty-prompt {:title (h2 "It seems like you don't have any tab at the moment.")
                         :body  (p "Start enjoying Shinsetsu by add or import your bookmarks")})
        (ui-tab (merge (nth tabs current-tab-idx) {:ui/tabs tabs :ui/search-query search-query}))))))

(defsc TabMain
  [this {:ui/keys [edit-tab-id delete-tab-id] :as props}]
  {:ident         (fn [] [:component/id ::tab])
   :route-segment ["tab"]
   :query         [{[:ui/tabs '_] (comp/get-query Tab)}
                   [:ui/search-query '_]
                   [:ui/search-error-type '_]
                   :ui/current-tab-idx
                   :ui/edit-tab-id
                   :ui/delete-tab-id
                   :ui/tab-ctx-menu-id
                   :ui/move-bm-id
                   :ui/destination-tab-id
                   :ui/loading?]
   :initial-state {:ui/current-tab-idx 0
                   :ui/loading?        false}
   :will-enter    (fn [app _]
                    (log/info "Loading user tabs")
                    (dr/route-deferred
                      [:component/id ::tab]
                      #(let [load-target (targeting/replace-at [:ui/tabs])]
                         ;; FIXME: Needs to load from local storage first before fetching from remote.
                         (df/load! app :user/tabs Tab {:target               load-target
                                                       :without              #{:tab/bookmarks}
                                                       :post-mutation        `dr/target-ready
                                                       :post-mutation-params {:target [:component/id ::tab]}}))))}
  [(when edit-tab-id
     (if (tempid/tempid? edit-tab-id)
       (ui-new-tab-modal this props)
       (ui-edit-tab-modal this props)))
   (when delete-tab-id
     (ui-delete-tab-modal this props))
   (ui-tab-main-body this props)])
