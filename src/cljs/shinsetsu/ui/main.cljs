(ns shinsetsu.ui.main
  (:require
    [shinsetsu.ui.elastic :as e]
    [shinsetsu.ui.tab :refer [TabModal Tab ui-tab-modal ui-tab]]
    [shinsetsu.application :refer [app]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :refer [div label input form button h1 h2 nav h5 p span]]
    [com.fulcrologic.fulcro.mutations :as m]
    [shinsetsu.mutations.common :refer [remove-ident]]
    [shinsetsu.mutations.tab :refer [delete-tab]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
    [com.fulcrologic.fulcro.dom.events :as evt]))

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

(defn- ui-main-body
  [this tabs selected-tab-idx]
  (e/page-template
    {:pageHeader {:pageTitle      (if (empty? tabs)
                                    "Welcome!"
                                    (-> tabs (nth selected-tab-idx) :tab/name))
                  :rightSideItems [(e/button-icon
                                     {:fill     true
                                      :size     "m"
                                      :onClick  (fn []
                                                  (m/set-value! this :ui/show-tab-modal? true)
                                                  (merge/merge-component!
                                                    app TabModal (comp/get-initial-state TabModal)
                                                    :append [:component/id ::main :user/tabs]))
                                      :iconType "plus"})
                                   (if (not (empty? tabs))
                                     (e/button-icon
                                       {:fill     true
                                        :size     "m"
                                        :onClick  #(m/set-value! this :ui/show-edit-modal? true)
                                        :iconType "pencil"}))
                                   (if (not (empty? tabs))
                                     (e/button-icon
                                       {:fill     true
                                        :size     "m"
                                        :color    "danger"
                                        :onClick  #(m/set-value! this :ui/show-delete-modal? true)
                                        :iconType "trash"}))
                                   (e/button-icon
                                     {:fill     true
                                      :size     "m"
                                      :iconType "importAction"})]}}
    (e/tabs {:size "xl"}
      (ui-tab-headers this tabs selected-tab-idx))
    (if (empty? tabs)
      (e/empty-prompt {:title (h2 "It seems like you don't have any tab at the moment.")
                       :body  (p "Start enjoying Shinsetsu by add or import your bookmarks")})
      (ui-tab (nth tabs selected-tab-idx)))))

(defsc Main
  [this {:user/keys [tabs] :ui/keys [selected-tab-idx show-tab-modal? show-edit-modal? show-delete-modal?]}]
  {:ident         (fn [] [:component/id ::main])
   :route-segment ["main"]
   :query         [{:user/tabs (comp/get-query Tab)}
                   :ui/selected-tab-idx
                   :ui/show-edit-modal?
                   :ui/show-delete-modal?
                   :ui/show-tab-modal?]
   :initial-state {:user/tabs             []
                   :ui/selected-tab-idx   0
                   :ui/show-tab-modal?    false
                   :ui/show-delete-modal? false
                   :ui/show-edit-modal?   false}
   :will-enter    (fn [app _]
                    (log/info "Loading user tabs")
                    (let [load-target         (targeting/append-to [:component/id ::main :user/tabs])
                          target-ready-params {:target [:component/id ::main]}]
                      ;; FIXME: Needs to load from local storage first before fetching from remote.
                      (dr/route-deferred
                        [:component/id ::main]
                        #(df/load! app :user/tabs Tab {:target               load-target
                                                       :post-mutation        `dr/target-ready
                                                       :post-mutation-params target-ready-params}))))}
  [(if show-tab-modal?
     (ui-new-tab this tabs))
   (if show-edit-modal?
     (ui-edit-tab this (nth tabs selected-tab-idx)))
   (if show-delete-modal?
     (ui-delete-tab this (nth tabs selected-tab-idx)))
   (as-> tabs $
         (filter #(not (tempid/tempid? (:tab/id %))) $)
         (ui-main-body this $ selected-tab-idx))])
