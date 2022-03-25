(ns shinsetsu.ui.tab
  (:require
    [shinsetsu.application :refer [app]]
    [shinsetsu.ui.elastic :as e]
    [shinsetsu.mutations.tab :as tab-mut]
    [shinsetsu.mutations.bookmark :as bookmark-mut]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.fulcro.dom :refer [div label input form button h1 h2 nav h5 p]]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.fulcro.routing.dynamic-routing :refer [defrouter]]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [shinsetsu.schema :as s]
    [taoensso.timbre :as log]
    [malli.core :as mc]
    [com.fulcrologic.fulcro.algorithms.tempid :as tempid]))

(defn tab-valid?
  [{:tab/keys [name password is-protected?] :as tab} tab-field]
  (mc/validate s/tab-form-spec tab))

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

(defsc TabBookmark
  [this {:bookmark/keys [id title url] :as bookmark}]
  {:ident :bookmark/id
   :query [:bookmark/id :bookmark/title :bookmark/url]}
  (div title))

(def ui-tab-bookmark (comp/factory TabBookmark {:keyfn :bookmark/id}))

(defsc TabBody
  [this {:tab/keys [id is-protected?] :ui/keys [bookmarks password is-unlocked? error-type] :as props}]
  {:ident         :tab/id
   :query         [:tab/id
                   :tab/name
                   :tab/password
                   :tab/is-protected?
                   {:ui/bookmarks (comp/get-query TabBookmark)}
                   :ui/is-unlocked?
                   :ui/password
                   :ui/error-type]
   :initial-state {:ui/bookmarks    []
                   :ui/password     ""
                   :ui/is-unlocked? false}}
  (let [on-unlock    #(do
                        (m/set-value! this :ui/is-unlocked? true)
                        (if (or (nil? password) (= "" password))
                          (comp/transact! this [(bookmark-mut/fetch-bookmarks #:tab{:id id})])
                          (comp/transact! this [(bookmark-mut/fetch-bookmarks #:tab{:id id :password password})])))
        add-bm-btn   (e/button {:fill true :iconType "plus"} "Add new bookmark")
        bookmark-uis #(if (empty? bookmarks)
                        (e/empty-prompt {:title   (p "Seems like you don't have any bookmark")
                                         :body    (p "Let's add your first bookmark!")
                                         :actions [add-bm-btn]})
                        (e/page-template {:pageHeader {:pageTitle      "Welcome!"
                                                       :rightSideItems [add-bm-btn]}}
                          (map ui-tab-bookmark bookmarks)))]
    (if is-protected?
      (if is-unlocked?
        (bookmark-uis)
        (e/empty-prompt {:title   (h2 "This tab is protected!")
                         :body    (e/form {:component "form" :id "tab-password-modal"}
                                    (e/form-row {:label "Password"}
                                      (e/field-text {:type     "password"
                                                     :name     "password"
                                                     :value    password
                                                     :onChange #(m/set-string! this :ui/password :event %)})))
                         :actions [(e/button {:fill true :onClick on-unlock} "UNLOCK")]}))
      (do
        (on-unlock)
        (bookmark-uis)))))

(def ui-tab-body (comp/factory TabBody {:keyfn :tab/id}))
