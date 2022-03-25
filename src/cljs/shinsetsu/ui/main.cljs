(ns shinsetsu.ui.main
  (:require
    [shinsetsu.ui.elastic :as e]
    [shinsetsu.ui.tab :as tab-ui]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom :refer [div label input form button h1 h2 nav h5 p span]]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [clojure.string :as string]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.algorithms.tempid :as tempid]))

(defsc Main
  [this {:user/keys [tabs] :ui/keys [selected-tab-idx loading? show-modal? tab-modal-data]}]
  {:ident         (fn [] [:component/id ::main])
   :route-segment ["main"]
   :query         [{:user/tabs (comp/get-query tab-ui/TabBody)}
                   :ui/selected-tab-idx
                   :ui/loading?
                   :ui/show-modal?
                   {:ui/tab-modal-data (comp/get-query tab-ui/TabModal)}]
   :initial-state (fn [_]
                    {:user/tabs           []
                     ;; FIXME: Do proper loading.
                     :ui/loading?         false
                     :ui/selected-tab-idx 0
                     :ui/show-modal?      false
                     :ui/tab-modal-data   (comp/get-initial-state tab-ui/TabModal)})
   :will-enter    (fn [app _]
                    (let [load-target         (targeting/append-to [:component/id ::main :user/tabs])
                          target-ready-params {:target [:component/id ::main]}]
                      ;; FIXME: Needs to load from local storage first before fetching from remote.
                      (dr/route-deferred
                        [:component/id ::main]
                        #(df/load! app :user/tabs tab-ui/TabBody {:target               load-target
                                                                  :post-mutation        `dr/target-ready
                                                                  :post-mutation-params target-ready-params}))))}
  (if show-modal?
    (let [on-close #(m/set-value! this :ui/show-modal? false)]
      (tab-ui/ui-tab-modal (comp/computed tab-modal-data {:on-close on-close})))
    (let [ui-tabs (map-indexed (fn [i {:tab/keys [id name is-protected?] :ui/keys [is-unlocked?]}]
                                 {:id         id
                                  :label      (h2 (str name " ") (if is-protected?
                                                                   (if is-unlocked?
                                                                     (e/icon {:type "lockOpen"})
                                                                     (e/icon {:type "lock"}))))
                                  :onClick    #(m/set-integer! this :ui/selected-tab-idx :value i)
                                  :isSelected (= i selected-tab-idx)}) tabs)]
      (e/page-template {:pageHeader {:pageTitle      "Welcome!"
                                     :rightSideItems [(e/button {:fill     true
                                                                 :onClick  #(m/set-value! this :ui/show-modal? true)
                                                                 :iconType "plus"}
                                                        "Create new tab")
                                                      (e/button {:fill     true
                                                                 :iconType "importAction"}
                                                        "Import bookmarks")]
                                     :tabs           ui-tabs}}
        (if (empty? tabs)
          (e/empty-prompt {:title (h2 "It seems like you don't have any tab at the moment.")
                           :body  (p "Start enjoying Shinsetsu by add or import your bookmarks")})
          (let [selected-tab (nth tabs selected-tab-idx)]
            (tab-ui/ui-tab-body selected-tab)))))))
