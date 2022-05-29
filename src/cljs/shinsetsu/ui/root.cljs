(ns shinsetsu.ui.root
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :refer [div h2 p span]]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
    [shinsetsu.store :refer [get-key store]]
    [shinsetsu.ui.elastic :as e]
    [shinsetsu.ui.login :refer [Login]]
    [shinsetsu.ui.tab :refer [TabMain]]
    [shinsetsu.ui.tag :refer [TagMain]]))

(defrouter RootRouter
  [_ {:keys [current-state]}]
  {:router-targets [Login TabMain TagMain]}
  (case current-state
    :failed (e/empty-prompt {:iconType "alert"
                             :color    "danger"
                             :title    (h2 "Unable to load your dashboard")
                             :body     (p "There has been an error loading the dashboard. Please try again later.")})
    (e/empty-prompt {:icon  (e/loading-spinner {:size "xxl"})
                     :title (h2 "Loading...")})))

(def ui-root-router (comp/factory RootRouter))

(defn ui-sidebar
  [this props]
  (let [{:ui/keys [sidebar-open?]} props]
    (e/collapsible-nav
      {:isOpen  sidebar-open?
       :onClose #(m/set-value! this :ui/sidebar-open? false)
       :button  (e/header-section-item-button
                  {:onClick #(m/toggle! this :ui/sidebar-open?)}
                  (e/icon {:type "menu" :size "l"}))}
      (e/flex-item {:grow false}
        (e/collapsible-nav-group {:isCollapsible false}
          (e/list-group {:size      "l"
                         :listItems [{:label    "Tabs"
                                      :iconType "apps"
                                      :onClick  #(dr/change-route! this ["tab"])}
                                     {:label    "Tags"
                                      :iconType "tag"
                                      :onClick  #(dr/change-route! this ["tag"])}
                                     {:label    "Session"
                                      :iconType "tableDensityExpanded"
                                      :onClick  #(dr/change-route! this ["session"])}]})))
      (e/horizontal-rule {:margin "none"})
      (e/flex-item {:grow false}
        (e/collapsible-nav-group {:isCollapsible false}
          (e/list-group {:size      "l"
                         :listItems [{:label    "Settings"
                                      :iconType "gear"
                                      :onClick  #(dr/change-route! this ["settings"])}]}))))))

(defonce cache (e/create-cache))

(defsc RootBody
  [this {:root/keys [router] :ui/keys [dark-mode?] :as props}]
  {:ident         (fn [] [:component/id ::root])
   :query         [:root/ready?
                   {:root/router (comp/get-query RootRouter)}
                   :ui/dark-mode?
                   :ui/sidebar-open?]
   :initial-state (fn [_]
                    {:root/router      (comp/get-initial-state RootRouter)
                     :ui/dark-mode?    true
                     :ui/sidebar-open? false})}
  (e/provider {:cache cache :colorMode (if dark-mode? "dark" "light")}
    (if (get-key @store :userToken)
      (e/header {:sections [{:items [(ui-sidebar this props)] :borders "right"}]}))
    (ui-root-router router)))

(def ui-root-body (comp/factory RootBody))

(defsc Root
  [_ {:root/keys [body-data]}]
  {:query         [{:root/body-data (comp/get-query RootBody)}]
   :initial-state (fn [_] {:root/body-data (comp/get-initial-state RootBody)})}
  (ui-root-body body-data))
