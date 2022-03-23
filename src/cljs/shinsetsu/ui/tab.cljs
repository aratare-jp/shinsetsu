(ns shinsetsu.ui.tab
  (:require
    [shinsetsu.ui.elastic :as e]
    [shinsetsu.mutations.tab :as tab-mut]
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

(def tab-validator (fs/make-validator tab-valid?))

(defsc TabModal
  [this
   {:tab/keys [id name is-protected? password created updated]
    :ui/keys  [loading? error-type]
    :as       props}
   {:keys [on-close]}]
  {:ident         (fn [] [:component/id ::tab-modal])
   :query         [:tab/id :tab/name :tab/password :tab/is-protected? :tab/created :tab/updated
                   :ui/loading? :ui/error-type fs/form-config-join]
   :form-fields   #{:tab/name :tab/password :tab/is-protected?}
   :initial-state {:tab/name          ""
                   :tab/password      ""
                   :tab/is-protected? false}
   :pre-merge     (fn [{:keys [data-tree]}] (fs/add-form-config TabModal data-tree))}
  (let [on-name-changed         (fn [e] (m/set-string! this :tab/name :event e))
        on-password-changed     (fn [e] (m/set-string! this :tab/password :event e))
        on-is-protected-changed (fn [e] (m/set-value! this :tab/is-protected? (not is-protected?)))
        on-blur                 (fn [f] (comp/transact! this [(fs/mark-complete! {:field f})]))
        name-invalid?           (= :invalid (tab-validator props :tab/name))
        password-invalid?       (= :invalid (tab-validator props :tab/password))
        tab-invalid?            (or name-invalid? password-invalid?)
        on-close                (fn [e]
                                  (evt/prevent-default! e)
                                  (comp/transact! this [(fs/reset-form! {:form-ident (comp/get-ident this)})])
                                  (on-close))
        on-tab-save             (fn [e]

                                  (m/set-value! this :ui/loading? true)
                                  (comp/transact! this [(tab-mut/create-tab {:tab/name          name
                                                                             :tab/password      password
                                                                             :tab/is-protected? is-protected?})]))
        on-clear                (fn [e]
                                  (evt/prevent-default! e)
                                  (comp/transact! this [(fs/reset-form! {:form-ident (comp/get-ident this)})]))
        modal-title             (if id "Edit tab" "Create new tab")
        errors                  (case error-type
                                  :invalid-input ["Unable to create new tab."
                                                  "Please try again."]
                                  :internal-server-error ["Unknown error encountered"]
                                  nil)]
    (e/modal {:onClose on-close}
      (e/modal-header {}
        (e/modal-header-title {}
          (h1 modal-title)))
      (e/modal-body {}
        (e/form {:component "form" :isInvalid (boolean errors) :error errors}
          (e/form-row {:label "Name"}
            (e/field-text {:name     "name"
                           :value    name
                           :onChange on-name-changed
                           :onBlur   #(on-blur :tab/name)
                           :disabled loading?}))
          (e/form-row {:label "Password"}
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
                   :disabled  tab-invalid?
                   :form      "tab-modal-form"} "Save")
        (e/button {:onClick on-clear} "Clear")))))

(def ui-tab-modal (comp/factory TabModal {:keyfn :tab/id}))

(defsc TabBookmark
  [this {:bookmark/keys [id title url created updated tab-id] :as bookmark}]
  {:ident (fn [] [:bookmark/id id])
   :query [:bookmark/tab-id :bookmark/id :bookmark/title :bookmark/url :bookmark/created :bookmark/updated]}
  (div title))

(def ui-tab-bookmark (comp/factory TabBookmark {:keyfn :bookmark/id}))

(defsc TabBody
  [this {:tab/keys [id bookmarks] :as props}]
  {:ident             :tab/id
   :query             [:tab/id :tab/name {:tab/bookmarks (comp/get-query TabBookmark)}]
   :initial-state     {:tab/bookmarks []}
   :componentDidMount (fn [this]
                        (let [props (comp/props this)
                              id    (:tab/id props)]
                          ;; TODO: Right now this means data is loaded every time the tab is selected -> performance issue
                          (df/load! this [:tab/id id] TabBody)))}
  (map ui-tab-bookmark bookmarks))

(def ui-tab-body (comp/factory TabBody {:keyfn :tab/id}))

(defsc TabHeaders
  [_ _]
  {:ident :tab/id
   :query [:tab/id :tab/name]})

