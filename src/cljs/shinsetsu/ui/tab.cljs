(ns shinsetsu.ui.tab
  (:require
    [shinsetsu.application :refer [app]]
    [shinsetsu.ui.elastic :as e]
    [shinsetsu.mutations.tab :as tab-mut]
    [shinsetsu.mutations.bookmark :as bookmark-mut]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.mutations :as m]
    [shinsetsu.ui.bookmark :as bookmark-ui]
    [com.fulcrologic.fulcro.dom :refer [div label input form button h1 h2 nav h5 p]]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.fulcro.routing.dynamic-routing :refer [defrouter]]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [shinsetsu.schema :as s]
    [taoensso.timbre :as log]
    [malli.core :as mc]
    [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]))

(defsc TabModal
  [this {:tab/keys [id name password] :ui/keys [loading? error-type]} {:keys [on-close]}]
  {:ident         :tab/id
   :query         [:tab/id :tab/name :tab/password :ui/loading? :ui/error-type fs/form-config-join]
   :form-fields   #{:tab/name :tab/password}
   :initial-state {:tab/name "" :tab/password ""}
   :pre-merge     (fn [{:keys [data-tree]}] (fs/add-form-config TabModal data-tree))}
  (let [on-name-changed     (fn [e] (m/set-string! this :tab/name :event e))
        on-password-changed (fn [e] (m/set-string! this :tab/password :event e))
        on-blur             (fn [f] (comp/transact! this [(fs/mark-complete! {:field f})]))
        tab-valid?          (mc/validate s/tab-form-spec #:tab{:name name :password password})
        on-close            (fn [_]
                              (comp/transact! this [(fs/reset-form! {:form-ident (comp/get-ident this)})])
                              (on-close))
        on-tab-save         #(let [args (if (= "" password)
                                          #:tab{:id id :name name}
                                          #:tab{:id id :name name :password password})]
                               (comp/transact! this [(tab-mut/create-tab args)]))
        on-clear            #(comp/transact! this [(fs/reset-form! {:form-ident (comp/get-ident this)})])
        errors              (case error-type
                              :invalid-input ["Unable to create new tab." "Please try again."]
                              :internal-server-error ["Unknown error encountered"]
                              nil)]
    (e/modal {:onClose on-close}
      (e/modal-header {}
        (e/modal-header-title {}
          (h1 (if id "Edit Tab" "Create New Tab"))))
      (e/modal-body {}
        (e/form {:component "form" :isInvalid (boolean errors) :error errors}
          (e/form-row {:label "Name"}
            (e/field-text {:name     "name"
                           :value    name
                           :onChange on-name-changed
                           :onBlur   #(on-blur :tab/name)
                           :disabled loading?}))
          (e/form-row {:label "Password" :helpText "Can be left empty if you don't want to lock this tab"}
            (e/field-text {:name     "password"
                           :value    password
                           :type     "password"
                           :onBlur   #(on-blur :tab/password)
                           :disabled loading?
                           :onChange on-password-changed}))))
      (e/modal-footer {}
        (e/button {:onClick on-close} "Cancel")
        (e/button {:type      "submit"
                   :fill      true
                   :onClick   on-tab-save
                   :isLoading loading?
                   :disabled  (not tab-valid?)
                   :form      "tab-modal-form"} "Save")
        (e/button {:onClick on-clear} "Clear")))))

(def ui-tab-modal (comp/factory TabModal {:keyfn :tab/id}))

(defsc TabBody
  [this {:tab/keys [id is-protected? bookmarks] :ui/keys [password unlocked? error-type show-bookmark-modal?]}]
  {:ident :tab/id
   :query [:tab/id
           :tab/name
           :tab/is-protected?
           {:tab/bookmarks (comp/get-query bookmark-ui/Bookmark)}
           :ui/unlocked?
           :ui/password
           :ui/error-type
           :ui/show-bookmark-modal?]}
  (let [unlock       (fn []
                       (m/set-value! this :ui/unlocked? true)
                       (if (or (nil? password) (= "" password))
                         (comp/transact! this [(bookmark-mut/fetch-bookmarks #:tab{:id id})])
                         (comp/transact! this [(bookmark-mut/fetch-bookmarks #:tab{:id id :password password})])))
        add-bm-btn   (e/button {:fill     true
                                :iconType "plus"
                                :onClick  (fn []
                                            (merge/merge-component! app bookmark-ui/BookmarkModal
                                                                    #:bookmark{:id (tempid/tempid) :title "" :url ""}
                                                                    :append [:tab/id id :tab/bookmarks])
                                            (m/set-value! this :ui/show-bookmark-modal? true))}
                       "Add new bookmark")
        bookmark-uis #(if (empty? bookmarks)
                        (e/empty-prompt {:title   (p "Seems like you don't have any bookmark")
                                         :body    (p "Let's add your first bookmark!")
                                         :actions [add-bm-btn]})
                        (e/page-template {:pageHeader {:pageTitle "Welcome!" :rightSideItems [add-bm-btn]}}
                          (map bookmark-ui/ui-bookmark bookmarks)))
        back-fn      #(m/set-value! this :ui/error-type nil)]
    (cond
      show-bookmark-modal?
      (let [new-bookmark (last bookmarks)
            on-close     (fn []
                           (comp/transact! this [(tab-mut/remove-ident
                                                   {:ident       (comp/get-ident bookmark-ui/BookmarkModal new-bookmark)
                                                    :remove-from (conj (comp/get-ident this) :ui/bookmarks)})])
                           (m/set-value! this :ui/show-bookmark-modal? false))]
        (bookmark-ui/ui-bookmark-modal (comp/computed new-bookmark {:on-close on-close})))
      error-type
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
                         :actions  [(e/button {:fill true :onClick back-fn} "Back")]}))
      is-protected?
      (if unlocked?
        (bookmark-uis)
        (e/empty-prompt {:title   (h2 "This tab is protected!")
                         :body    (e/form {:component "form" :id "tab-password-modal"}
                                    (e/form-row {:label "Password"}
                                      (e/field-text {:type     "password"
                                                     :name     "password"
                                                     :value    password
                                                     :onChange #(m/set-string! this :ui/password :event %)})))
                         :actions [(e/button {:fill true :onClick unlock} "Unlock this tab!")]}))
      :else
      (do
        (unlock)
        (bookmark-uis)))))

(def ui-tab-body (comp/factory TabBody {:keyfn :tab/id}))
