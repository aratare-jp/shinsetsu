(ns shinsetsu.ui.root
  (:require
    [shinsetsu.ui.elastic :as e]
    [shinsetsu.application :refer [app]]
    [shinsetsu.ui.login :refer [Login]]
    [shinsetsu.ui.tab :refer [TabMain]]
    [shinsetsu.ui.tag :refer [TagMain]]
    [shinsetsu.store :refer [store get-key]]
    [com.fulcrologic.fulcro.dom :refer [div h2 p]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :refer [defrouter]]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]))

(defrouter RootRouter
  [_ {:keys [current-state]}]
  {:router-targets [Login TabMain TagMain]}
  (case current-state
    :pending (e/empty-prompt {:icon  (e/loading-spinner {:size "xxl"})
                              :title (h2 "Loading...")})
    :failed (e/empty-prompt {:iconType "alert"
                             :color    "danger"
                             :title    (h2 "Unable to load your dashboard")
                             :body     (p "There has been an error loading the dashboard. Please try again later.")})
    (e/empty-prompt {:iconType "alert"
                     :color    "danger"
                     :title    (h2 "Unknown error")
                     :body     (p "Unknown error encountered. There has been something fishy going on here!")})))

(def ui-root-router (comp/factory RootRouter))

(defsc RootBody
  [this
   {:root/keys [router]
    :ui/keys   [dark-mode? selected-idx]}]
  {:ident         (fn [] [:component/id ::root])
   :query         [:root/ready?
                   {:root/router (comp/get-query RootRouter)}
                   :ui/dark-mode?
                   :ui/selected-idx]
   :initial-state (fn [_]
                    {:root/router     (comp/get-initial-state RootRouter)
                     :ui/dark-mode?   true
                     :ui/selected-idx "tab"})}
  (e/provider {:colorMode (if dark-mode? "dark" "light")}
    (if (get-key @store :userToken)
      (e/panel {}
        (e/flex-group {}
          (e/flex-item {})
          (e/flex-item {}
            (div
              (e/button-group
                {:legend      "Main menu"
                 :isFullWidth true
                 :buttonSize  "s"
                 :idSelected  selected-idx
                 :options     [{:id "tab" :label "Tab" :value "tab"}
                               {:id "tag" :label "Tag" :value "tag"}]
                 :onChange    (fn [id value]
                                (m/set-value! this :ui/selected-idx id)
                                (dr/change-route app [value]))})))
          (e/flex-item {}))))
    (ui-root-router router)))

(def ui-root-body (comp/factory RootBody))

(defsc Root
  [_ {:root/keys [body-data]}]
  {:query         [{:root/body-data (comp/get-query RootBody)}]
   :initial-state (fn [_] {:root/body-data (comp/get-initial-state RootBody)})}
  (ui-root-body body-data))
