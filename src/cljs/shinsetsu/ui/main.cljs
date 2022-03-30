(ns shinsetsu.ui.main
  (:require
    [shinsetsu.ui.elastic :as e]
    [shinsetsu.ui.tab :refer [TabModal Tab ui-tab-modal ui-tab]]
    [shinsetsu.application :refer [app]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :refer [div label input form button h1 h2 nav h5 p span]]
    [com.fulcrologic.fulcro.mutations :as m]
    [shinsetsu.mutations.common :refer [remove-ident]]
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
      {:id            id
       :prepend       (if is-protected?
                        (if unlocked?
                          (e/icon {:type "lockOpen"})
                          (e/icon {:type "lock"})))
       :onContextMenu (fn [e]
                        (evt/prevent-default! e)
                        (js/console.log "Hello"))
       :label         (h1 name)
       :onClick       #(m/set-integer! this :ui/selected-tab-idx :value i)
       :isSelected    (= i selected-tab-idx)})
    tabs))

(defn- ui-new-tab
  [this tabs]
  (let [new-tab  (first (filter #(tempid/tempid? (:tab/id %)) tabs))
        on-close (fn []
                   (comp/transact! this [(remove-ident {:ident (comp/get-ident TabModal new-tab)})])
                   (m/set-value! this :ui/show-tab-modal? false))]
    (ui-tab-modal (comp/computed new-tab {:on-close on-close}))))

(defn- ui-tabs
  [this tabs selected-tab-idx]
  (e/page-template
    {:pageHeader {:pageTitle      "Welcome!"
                  :rightSideItems [(e/button
                                     {:fill     true
                                      :onClick  (fn []
                                                  (m/set-value! this :ui/show-tab-modal? true)
                                                  (merge/merge-component!
                                                    app TabModal (comp/get-initial-state TabModal)
                                                    :append [:component/id ::main :user/tabs]))
                                      :iconType "plus"}
                                     "Create Tab")
                                   (e/button {:fill true :iconType "importAction"} "Import")]
                  :tabs           (ui-tab-headers this tabs selected-tab-idx)}}
    (if (empty? tabs)
      (e/empty-prompt {:title (h2 "It seems like you don't have any tab at the moment.")
                       :body  (p "Start enjoying Shinsetsu by add or import your bookmarks")})
      (ui-tab (nth tabs selected-tab-idx)))))

(defsc Main
  [this {:user/keys [tabs] :ui/keys [selected-tab-idx show-tab-modal?]}]
  {:ident         (fn [] [:component/id ::main])
   :route-segment ["main"]
   :query         [{:user/tabs (comp/get-query Tab)} :ui/selected-tab-idx :ui/show-tab-modal?]
   :initial-state {:user/tabs           []
                   :ui/selected-tab-idx 0
                   :ui/show-tab-modal?  false}
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
   (as-> tabs $
         (filter #(not (tempid/tempid? (:tab/id %))) $)
         (ui-tabs this $ selected-tab-idx))])
