(ns shinsetsu.ui.main
  (:require
    [shinsetsu.ui.elastic :as e]
    [shinsetsu.mutations :as api]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom :refer [div label input form button h1 h2 nav h5]]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]
    [clojure.string :as string]
    [taoensso.timbre :as log]))

(defn tab-valid?
  [{:ui/keys [name]} field]
  (let [not-empty? (complement empty?)]
    (case field
      :ui/name (not-empty? name)
      false)))

(def tab-validator (fs/make-validator tab-valid?))

(defsc TabModal
  [this {:ui/keys [id name created updated] :as props}]
  {:ident         (fn [] [:component/id ::tab-modal])
   :query         [:ui/id :ui/name :ui/created :ui/updated fs/form-config-join]
   :form-fields   #{:ui/name}
   :initial-state {:ui/name ""}
   :pre-merge     (fn [{:keys [data-tree]}] (fs/add-form-config TabModal data-tree))}
  (let [on-name-changed (fn [e] (m/set-string! this :ui/name :event e))
        on-blur         (fn [f] (comp/transact! this [(fs/mark-complete! {:field f})]))
        name-invalid?   (= :invalid (tab-validator props :ui/name))
        tab-invalid?    name-invalid?
        on-tab-save     (fn [e]
                          (evt/prevent-default! e)
                          (comp/transact! this [(api/create-tab {:tab/name name})]))
        on-clear        (fn [e]
                          (evt/prevent-default! e)
                          (comp/transact! this [(fs/reset-form! {:form-ident (comp/get-ident this)})]))]
    (div :.modal.fade#tab-modal {:tabIndex -1}
         (div :.modal-dialog.modal-dialog-centered.modal-dialog-scrollable
              (div :.modal-content
                   (form
                     (div :.modal-header
                          (h5 :.modal-title#tab-modal-label (str (if id "Edit" "Create") " tab"))
                          (button :.btn-close {:type            "button"
                                               :data-bs-dismiss "modal"}))
                     (div :.modal-body
                          (div :.form-floating.mb-3
                               (input :#tab-name.form-control.form-control-lg
                                      {:classes     [(if name-invalid? "is-invalid")]
                                       :value       name
                                       :onChange    on-name-changed
                                       :onBlur      #(on-blur :ui/name)
                                       :placeholder "Name"})
                               (label :.form-label {:htmlFor "tab-name"} "Name")))
                     (div :.modal-footer
                          (button :.btn.btn-secondary.btn-lg {:onClick on-clear} "Clear")
                          (button :.btn.btn-primary.btn-lg {:type     "submit"
                                                            :onClick  on-tab-save
                                                            :disabled tab-invalid?} "Save"))))))))

(def ui-tab-modal (comp/factory TabModal))

(defsc TabBookmark
  [this {:bookmark/keys [id title url created updated tab-id] :as bookmark}]
  {:ident (fn [] [:bookmark/id id])
   :query [:bookmark/tab-id :bookmark/id :bookmark/title :bookmark/url :bookmark/created :bookmark/updated]}
  (div title))

(def ui-tab-bookmark (comp/factory TabBookmark {:keyfn :bookmark/id}))

(defsc TabBody
  [this {:tab/keys [id bookmarks] :as props}]
  {:ident         :tab/id
   :query         [:tab/id :tab/name {:tab/bookmarks (comp/get-query TabBookmark)}]
   :route-segment [:tab :tab/id]
   :will-enter    (fn [app {:tab/keys [id]}]
                    (dr/route-deferred
                      [:tab id]
                      #(df/load! app [:tab/id id] TabBody {:target               (targeting/replace-at [:tab/id id :tab/bookmarks])
                                                           :post-mutation        `dr/target-ready
                                                           :post-mutation-params {:target [:tab id]}})))}
  (div props)
  #_(map ui-tab-bookmark bookmarks))

(def ui-tab-body (comp/factory TabBody {:keyfn :tab/id}))

(defrouter TabBodyRouter [this {:keys [current-state pending-path-segment] :as env}]
  {:router-targets [TabBody]}
  (js/console.log env)
  (case current-state
    :pending (dom/div "Loading...")
    :failed (dom/div "Loading seems to have failed. Try another route.")
    (dom/div "Unknown route")))

(def ui-body-router (comp/factory TabBodyRouter))

(defsc Main
  [this {tabs :tab/tabs :ui/keys [tab-modal selected-tab-idx] tab-body-router :root/tab-body-router :as props}]
  {:ident         (fn [] [:component/id ::main])
   :route-segment ["main"]
   :query         [{:tab/tabs (comp/get-query TabBody)}
                   {:ui/tab-modal (comp/get-query TabModal)}
                   :ui/selected-tab-idx
                   {:root/tab-body-router (comp/get-query TabBodyRouter)}]
   :initial-state (fn [_] {:tab/tabs             []
                           :ui/tab-modal         (comp/get-initial-state TabModal)
                           :ui/selected-tab-idx  0
                           :root/tab-body-router (comp/get-initial-state TabBodyRouter)})
   :will-enter    (fn [app _]
                    (dr/route-deferred
                      [:component/id ::main]
                      #(df/load! app :tab/tabs TabBody {:target               (targeting/append-to [:component/id ::main :tab/tabs])
                                                        :post-mutation        `dr/target-ready
                                                        :post-mutation-params {:target [:component/id ::main]}})))}
  (let [tabs (map-indexed (fn [i t] {:id         (:tab/id t)
                                     :label      (:tab/name t)
                                     :onClick    #(do (m/set-integer! this :ui/selected-tab-idx :value i)
                                                      (dr/change-route-relative! this this [:tab (:tab/id t)]))
                                     :isSelected (= i selected-tab-idx)}) tabs)]
    (e/page-template {:pageHeader {:pageTitle      "Tabs"
                                   :rightSideItems [(e/button {:fill true} "Create new tab")]
                                   :tabs           tabs}}
      (ui-body-router tab-body-router))))

(def ui-main (comp/factory Main))
