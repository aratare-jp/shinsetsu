(ns shinsetsu.ui.main
  (:require
    [shinsetsu.mutations :as api]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom :refer [div label input form button h1 h2 nav]]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]
    [clojure.string :as string]))

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
  [this {tab-ids :tab/ids :as props}]
  {:ident         (fn [] [:component/id ::main])
   :route-segment ["main"]
   :query         [{:tab/ids (comp/get-query TabHeader)}]
   :initial-state {}
   :will-enter    (fn [app _]
                    (dr/route-deferred
                      [:component/id ::main]
                      #(df/load! app :tab/ids TabHeader {:remote               :protected
                                                         :target               (targeting/append-to [:component/id ::main :tab/ids])
                                                         :post-mutation        `dr/target-ready
                                                         :post-mutation-params {:target [:component/id ::main]}})))}
  (let [tab-ids (map-indexed (fn [i e] (if (= i 0) (assoc e :ui/is-first? true) e)) tab-ids)]
    (div
      (button :.btn.btn-primary.btn-lg
              {:onClick #(df/load! this :tab/ids TabHeader {:remote :protected
                                                            :target (targeting/replace-at [:component/id ::main :tab/ids])})}
              "Refresh")
      (nav
        (div :.nav.nav-tabs#nav-tab {:role "tablist"}
             (map ui-tab-header tab-ids)))
      (div :.tab-content#nav-tabContent
           (map ui-tab-body tab-ids)))))

(def ui-main (comp/factory Main))
