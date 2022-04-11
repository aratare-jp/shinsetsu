(ns shinsetsu.ui.tab
  (:require
    [shinsetsu.application :refer [app]]
    [clojure.browser.dom :refer [get-element]]
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
    [shinsetsu.mutations.common :refer [remove-ident set-root]]
    [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.dom.events :as evt]))

(defsc TabModal
  [this
   {:tab/keys [id name]
    :ui/keys  [password loading? error-type change-password?]}
   {:keys [on-close]}]
  {:ident         :tab/id
   :query         [:tab/id :tab/name
                   :ui/password :ui/loading? :ui/error-type :ui/change-password?
                   fs/form-config-join]
   :form-fields   #{:tab/name :ui/password :ui/change-password?}
   :initial-state (fn [_]
                    {:tab/id      (tempid/tempid)
                     :tab/name    ""
                     :ui/password ""})
   :pre-merge     (fn [{:keys [data-tree]}] (fs/add-form-config TabModal data-tree))}
  (let [new?        (tempid/tempid? id)
        on-blur     (fn [f] (comp/transact! this [(fs/mark-complete! {:field f})]))
        tab-valid?  (mc/validate s/tab-form-spec #:tab{:name name :password password})
        on-close    (fn [_]
                      (comp/transact! this [(fs/reset-form! {:form-ident (comp/get-ident this)})])
                      (on-close))
        on-save     (fn [e]
                      (evt/prevent-default! e)
                      (if new?
                        (comp/transact! this [(create-tab #:tab{:id id :name name :password password})])
                        (if change-password?
                          (comp/transact! this [(patch-tab #:tab{:id               id
                                                                 :name             name
                                                                 :password         password
                                                                 :change-password? true})])
                          (comp/transact! this [(patch-tab #:tab{:id id :name name})]))))
        on-clear    #(comp/transact! this [(fs/reset-form! {:form-ident (comp/get-ident this)})])
        errors      (case error-type
                      :invalid-input ["Unable to create new tab." "Please try again."]
                      :internal-server-error ["Unknown error encountered"]
                      nil)
        ui-password (e/form-row
                      {:label    "Password"
                       :helpText (if (not new?) "Can be left empty if you don't want to lock this tab")}
                      (e/field-text
                        {:name        "password"
                         :value       password
                         :placeholder "Add password here"
                         :type        "password"
                         :onBlur      #(on-blur :ui/password)
                         :disabled    loading?
                         :onChange    (fn [e] (m/set-string! this :ui/password :event e))}))]
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
               :onChange    #(m/set-string! this :tab/name :event %)
               :onBlur      #(on-blur :tab/name)
               :disabled    loading?}))
          (e/form-row {:label (if new? "Add password?" "Change password?")}
            (e/switch
              {:name     "change-password"
               :checked  change-password?
               :onChange #(m/toggle! this :ui/change-password?)}))
          (if change-password?
            ui-password)))
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
  [this id selected-idx bookmarks]
  (if selected-idx
    (let [bookmark (merge (nth bookmarks selected-idx) {:bookmark/tab-id id})
          on-close (fn []
                     (m/set-value! this :ui/show-bookmark-modal? false)
                     (m/set-value! this :ui/selected-idx nil))]
      (bui/ui-bookmark-modal (comp/computed bookmark {:on-close on-close})))
    (let [new-bookmark (first (filter (fn [b] (tempid/tempid? (:bookmark/id b))) bookmarks))
          bm-ident     (comp/get-ident bui/BookmarkModal new-bookmark)
          on-close     (fn []
                         (m/set-value! this :ui/show-bookmark-modal? false)
                         (comp/transact! this [(remove-ident {:ident bm-ident})]))]
      (bui/ui-bookmark-modal (comp/computed new-bookmark {:on-close on-close})))))

(defn- ui-tab-body
  [this]
  (let [{:tab/keys [id bookmarks] :ui/keys [loading?]} (comp/props this)
        add-bm-fn  (fn []
                     (merge/merge-component!
                       app bui/BookmarkModal
                       (comp/get-initial-state bui/BookmarkModal {:bookmark/tab-id id})
                       :append [:tab/id id :tab/bookmarks])
                     (m/set-value! this :ui/show-bookmark-modal? true))
        add-bm-btn (e/button {:fill true :iconType "plus" :onClick add-bm-fn} "Add Bookmark")]
    [(if loading?
       (e/progress {:size "xs" :color "ascent"}))
     (if (empty? bookmarks)
       (e/empty-prompt {:title   (p "Seems like you don't have any bookmark")
                        :body    (p "Let's add your first bookmark!")
                        :actions [add-bm-btn]})
       (e/flex-grid {:columns 3}
         (->> bookmarks
              (filter (fn [b] (not (tempid/tempid? (:bookmark/id b)))))
              (map-indexed
                (fn [i {bookmark-id :bookmark/id :as bookmark}]
                  (let [on-click (fn []
                                   (m/set-integer! this :ui/selected-idx :value i)
                                   (m/set-value! this :ui/show-bookmark-modal? true))
                        bookmark (merge bookmark {:bookmark/tab-id id})]
                    (e/flex-item {:key bookmark-id}
                      (bui/ui-bookmark (comp/computed bookmark {:on-click on-click})))))))
         (e/flex-item {:key "new-bookmark"}
           (bui/ui-new-bookmark (comp/computed {} {:on-click add-bm-fn})))))]))

(defn- ui-unlock-prompt
  [this]
  (let [{:tab/keys [id] :ui/keys [loading? unlocked? password error-type]} (comp/props this)]
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
                                     (let [load-fn (fn []
                                                     (log/info "Load user bookmarks")
                                                     (comp/transact!
                                                       this
                                                       [{(fetch-bookmarks #:tab{:id id :password password})
                                                         [:tab/id {:tab/bookmarks (comp/get-query bui/Bookmark)}]}]))]
                                       (do
                                         (load-fn)
                                         ;; FIXME: Magic number!
                                         #_(m/set-value! this :ui/load-timer (js/setInterval load-fn 10000)))))}
                       "Unlock this tab!")]}))
      (ui-tab-body this))))

(defsc Tab
  [this
   {:tab/keys [id is-protected? bookmarks]
    :ui/keys  [show-bookmark-modal? selected-idx]}]
  {:ident                :tab/id
   :query                [:tab/id
                          :tab/name
                          :tab/is-protected?
                          {:tab/bookmarks (comp/get-query bui/BookmarkModal)}
                          :ui/unlocked?
                          :ui/password
                          :ui/change-password?
                          :ui/error-type
                          :ui/show-bookmark-modal?
                          :ui/selected-idx
                          :ui/loading?
                          :ui/load-timer]
   :componentWillMount   (fn [this]
                           (let [{:tab/keys [id is-protected?]} (comp/props this)
                                 load-fn (fn []
                                           (log/info "Load user bookmarks")
                                           (comp/transact!
                                             this
                                             [{(fetch-bookmarks #:tab{:id id})
                                               [:tab/id {:tab/bookmarks (comp/get-query bui/Bookmark)}]}]))]
                             (if (not is-protected?)
                               (do
                                 (load-fn)
                                 ;; FIXME: Magic number!
                                 #_(m/set-value! this :ui/load-timer (js/setInterval load-fn 10000))))))
   :componentWillUnmount (fn [this]
                           (if-let [tid (:ui/load-timer (comp/props this))]
                             (js/clearInterval tid))
                           (m/set-value! this :ui/load-timer nil))}
  [(e/spacer {})
   (if show-bookmark-modal?
     (ui-bookmark-modal this id selected-idx bookmarks))
   (cond
     is-protected? (ui-unlock-prompt this)
     :else (ui-tab-body this))])

(def ui-tab (comp/factory Tab {:keyfn :tab/id}))

(defn- show-if-unlocked
  ([tabs selected-idx btn] (show-if-unlocked tabs selected-idx btn true))
  ([tabs selected-idx btn show-if-public?]
   (if-let [curr-tab (and (not (empty? tabs)) (nth tabs selected-idx))]
     (let [is-protected? (:tab/is-protected? curr-tab)
           unlocked?     (:ui/unlocked? curr-tab)]
       (if is-protected?
         (if unlocked?
           btn)
         (if show-if-public?
           btn))))))

(defn- ui-tab-headers
  [this]
  (let [{:ui/keys [tabs selected-idx tab-ctx-menu-id]} (comp/props this)
        close-ctx-menu-fn #(m/set-value! this :ui/tab-ctx-menu-id nil)]
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
                                        (comp/transact! this [(lock-tab tab)])
                                        (close-ctx-menu-fn))})))
                   (e/flex-item {:grow false}
                     (e/button-icon
                       {:aria-label "edit"
                        :size       "s"
                        :iconType   "pencil"
                        :onClick    (fn []
                                      (m/set-value! this :ui/edit-id id)
                                      (close-ctx-menu-fn))}))
                   (e/flex-item {:grow false}
                     (e/button-icon
                       {:aria-label "delete"
                        :size       "s"
                        :iconType   "trash"
                        :onClick    (fn []
                                      (m/set-value! this :ui/show-delete-modal? true)
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
                             ;; Only allow context menu for unlocked or public tabs
                             (m/set-value! this :ui/tab-ctx-menu-id (:tab/id tab)))
            :onClick       #(m/set-integer! this :ui/selected-idx :value i)
            :isSelected    (= i selected-idx)}
           (div
             (div {:id (str "tab-" id)})
             (p name)))])
      tabs)))

(defn- ui-new-tab
  [this]
  (let [{:ui/keys [tabs]} (comp/props this)
        new-tab  (first (filter #(tempid/tempid? (:tab/id %)) tabs))
        on-close (fn []
                   (comp/transact! this [(remove-ident {:ident (comp/get-ident TabModal new-tab)})])
                   (m/set-value! this :ui/show-tab-modal? false))]
    (ui-tab-modal (comp/computed new-tab {:on-close on-close}))))

(defn ui-edit-tab
  [this]
  (let [{:ui/keys [tabs edit-id]} (comp/props this)
        tab (->> tabs (filter #(= edit-id (:tab/id %))) first)]
    (let [on-close #(m/set-value! this :ui/edit-id nil)]
      (ui-tab-modal (comp/computed tab {:on-close on-close})))))

(defn- ui-delete-tab
  [this]
  (let [{:ui/keys [tabs selected-idx]} (comp/props this)
        {:tab/keys [id name]} (nth tabs selected-idx)]
    (e/confirm-modal
      {:title             (str "Delete tab " name)
       :onCancel          #(m/set-value! this :ui/show-delete-modal? false)
       :onConfirm         #(comp/transact! this [(delete-tab {:tab/id id})])
       :cancelButtonText  "Cancel"
       :confirmButtonText "Yes, I'm sure!"
       :buttonColor       "danger"}
      (p "Deleting this tab will also delete all the bookmarks within it!")
      (p "Are you sure you want to delete this tab?"))))

(defn- ui-tab-main-body
  [this]
  (let [{:ui/keys [tabs selected-idx]} (comp/props this)
        tabs             (filter #(not (tempid/tempid? (:tab/id %))) tabs)
        right-side-items [(e/button
                            {:fill     true
                             :size     "m"
                             :onClick  (fn []
                                         (m/set-value! this :ui/show-tab-modal? true)
                                         (merge/merge-component!
                                           app TabModal (comp/get-initial-state TabModal)
                                           :append (conj (comp/get-ident this) :ui/tabs)))
                             :iconType "plus"}
                            "Create New Tab")]]
    (e/page-template
      {:pageHeader {:pageTitle      (if (empty? tabs)
                                      "Welcome!"
                                      (-> tabs (nth selected-idx) :tab/name))
                    :rightSideItems right-side-items}}
      (e/tabs {:size "xl"}
        (ui-tab-headers this))
      (if (empty? tabs)
        (e/empty-prompt {:title (h2 "It seems like you don't have any tab at the moment.")
                         :body  (p "Start enjoying Shinsetsu by add or import your bookmarks")})
        (ui-tab (nth tabs selected-idx))))))

(defsc TabMain
  [this {:ui/keys [show-tab-modal? show-delete-modal? edit-id]}]
  {:ident         (fn [] [:component/id ::tab])
   :route-segment ["tab"]
   :query         [{:ui/tabs (comp/get-query Tab)}
                   :ui/selected-idx
                   :ui/show-delete-modal?
                   :ui/show-tab-modal?
                   :ui/edit-id
                   :ui/tab-ctx-menu-id]
   :initial-state {:ui/tabs               []
                   :ui/selected-idx       0
                   :ui/show-tab-modal?    false
                   :ui/show-delete-modal? false}
   :will-enter    (fn [app _]
                    (log/info "Loading user tabs")
                    (dr/route-deferred
                      [:component/id ::tab]
                      #(let [load-target (targeting/replace-at [:component/id ::tab :ui/tabs])]
                         ;; FIXME: Needs to load from local storage first before fetching from remote.
                         (df/load! app :user/tabs Tab {:target               load-target
                                                       :post-mutation        `dr/target-ready
                                                       :post-mutation-params {:target [:component/id ::tab]}}))))}
  [(if show-tab-modal?
     (ui-new-tab this))
   (if edit-id
     (ui-edit-tab this))
   (if show-delete-modal?
     (ui-delete-tab this))
   (ui-tab-main-body this)])
