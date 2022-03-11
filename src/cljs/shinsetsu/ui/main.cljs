(ns shinsetsu.ui.main
  (:require
    [shinsetsu.mutations :as api]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom :refer [div label input form button h1 h2 nav h5]]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]
    [clojure.string :as string]))

(defn tab-valid?
  [{:tab/keys [name]} field]
  (let [not-empty? (complement empty?)]
    (case field
      :tab/name (not-empty? name)
      false)))

(def tab-validator (fs/make-validator tab-valid?))

(defsc TabModal
  [this {:tab/keys [id name] :as tab}]
  {:ident         (fn [] [:tab/id (:tab/id tab)])
   :query         [:tab/id :tab/name :tab/created :tab/updated fs/form-config-join]
   :form-fields   #{:tab/name}
   :initial-state (fn []
                    {:tab/name ""
                     :tab/id   (random-uuid)})
   :pre-merge     (fn [{:keys [data-tree]}] (fs/add-form-config TabModal data-tree))}
  (let [on-name-changed (fn [e] (m/set-string! this :tab/name :event e))
        on-blur         (fn [f] (comp/transact! this [(fs/mark-complete! {:field f})]))
        name-invalid?   (= :invalid (tab-validator tab :tab/name))
        tab-invalid?    name-invalid?
        on-tab-save     (fn [e]
                          (evt/prevent-default! e)
                          (comp/transact! this [(api/create-tab {:tab/name name})]))
        on-clear        (fn [e]
                          (evt/prevent-default! e)
                          (comp/transact! this [(fs/reset-form! {:form-ident (comp/get-ident this)})]))]
    (div :.modal.fade#tab-modal {:tabIndex -1}
         (div :.modal-dialog
              (div :.modal-content
                   (form
                     (div :.modal-header
                          (h5 :.modal-title#tab-modal-label "Create new tab")
                          (button :.btn-close {:type            "button"
                                               :data-bs-dismiss "modal"}))
                     (div :.modal-body
                          (div :.form-floating.mb-3
                               (input :#username.form-control.form-control-lg
                                      {:classes     [(if name-invalid? "is-invalid")]
                                       :value       name
                                       :onChange    on-name-changed
                                       :onBlur      #(on-blur :ui/username)
                                       :placeholder "Username"})
                               (label :.form-label {:htmlFor "username"} "Username")))
                     (div :.modal-footer
                          (button :.btn.btn-secondary.btn-lg {:data-bs-dismiss "modal"} "Close")
                          (button :.btn.btn-secondary.btn-lg {:onClick on-clear} "Clear")
                          (button :.btn.btn-primary.btn-lg {:type     "submit"
                                                            :onClick  on-tab-save
                                                            :disabled tab-invalid?} "Save"))))))))

(def ui-tab-modal (comp/factory TabModal))

(defsc TabHeader
  [this {:tab/keys [id name] :ui/keys [is-first?] :as tab}]
  {:ident (fn [] [:tab/id (:tab/id tab)])
   :query [:tab/id :tab/name :tab/created :tab/updated :ui/is-first?]}
  (button :.nav-link {:id             (string/join "-" ["nav" name "tab"])
                      :classes        [(if is-first? "active")]
                      :data-bs-toggle "tab"
                      :data-bs-target (string/join "-" ["#nav" name])
                      :type           "button"
                      :role           "tab"}
          name))

(def ui-tab-header (comp/factory TabHeader {:keyfn :tab/id}))

(defsc TabBody
  [this {:tab/keys [id name] :ui/keys [is-first?] :as tab}]
  {:ident (fn [] [:tab/id (:tab/id tab)])
   :query [:tab/id :tab/name :tab/created :tab/updated :ui/is-first?]}
  (div :.tab-pane.fade {:id      (string/join "-" ["nav" name])
                        :classes [(if is-first? "show") (if is-first? "active")]
                        :role    "tabpanel"}
       name))

(def ui-tab-body (comp/factory TabBody {:keyfn :tab/id}))

(defsc Main
  [this {tab-ids :tab/ids tab-modal :tab/modal :as props}]
  {:ident         (fn [] [:component/id ::main])
   :route-segment ["main"]
   :query         [{:tab/ids (comp/get-query TabHeader)}
                   {:tab/modal (comp/get-query TabModal)}]
   :initial-state {:tab/modal (comp/get-initial-state TabModal)}
   :will-enter    (fn [app _]
                    (dr/route-deferred
                      [:component/id ::main]
                      #(df/load! app :tab/ids TabHeader {:remote               :protected
                                                         :target               (targeting/append-to [:component/id ::main :tab/ids])
                                                         :post-mutation        `dr/target-ready
                                                         :post-mutation-params {:target [:component/id ::main]}})))}
  (let [tab-ids (map-indexed (fn [i e] (if (= i 0) (assoc e :ui/is-first? true) e)) tab-ids)]
    (div
      (button :.btn.btn-primary {:type           "button"
                                 :data-bs-toggle "modal"
                                 :data-bs-target "#tab-modal"}
              "Create tab")
      (ui-tab-modal tab-modal)
      (nav
        (div :.nav.nav-tabs#nav-tab {:role "tablist"}
             (map ui-tab-header tab-ids)))
      (div :.tab-content#nav-tabContent
           (map ui-tab-body tab-ids)))))

(def ui-main (comp/factory Main))
