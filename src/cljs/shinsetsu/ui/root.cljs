(ns shinsetsu.ui.root
  (:require
    [clojure.string :as string]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :refer [div h2 p span]]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
    [goog.functions :as gf]
    [shinsetsu.mutations.common :refer [remove-ident set-root]]
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

(defn- ui-search-bar
  [this {:ui/keys [search-error-type loading?]}]
  (let [schema {:strict true? :fields {:tag {:type "string"} :name {:type "string"}}}]
    (e/input-popover
      {:isOpen search-error-type
       :input  (e/search-bar
                 {:box      {:schema schema :incremental true :isLoading loading?}
                  :onChange (gf/debounce
                              (fn [q]
                                (let [{:keys [query error]} (js->clj q :keywordize-keys true)]
                                  (if error
                                    (comp/transact! this [(set-root {:ui/search-error-type error})])
                                    (comp/transact! this [(set-root {:ui/search-query      (e/query->EsQuery query)
                                                                     :ui/search-error-type nil})]))))
                              500)})}
      "Your query seems to be incorrect")))

(defn ui-sort-select
  [this {:ui/keys [sort-option]}]
  (let [str->opt     (fn [s]
                       (as-> s $
                             (string/split $ #"_")
                             (partition 2 $)
                             (mapv
                               (fn [i]
                                 (let [[k v] i]
                                   {:field (keyword "bookmark" k) :direction (keyword v)}))
                               $)))
        opt->str     (fn [o]
                       (as-> o o
                             (mapv
                               (fn [i]
                                 (str (-> i :field name) "_" (-> i :direction name)))
                               o)
                             (string/join "_" o)))
        sort-options [{:value "title_asc" :text "Title (A-Z)"}
                      {:value "title_desc" :text "Title (Z-A)"}
                      {:value "created_asc" :text "Created date (New to Old)"}
                      {:value "created_desc" :text "Created date (Old to New)"}
                      {:value "favourite_desc_created_asc" :text "Favourite first"}
                      {:value "favourite_asc_created_asc" :text "Non-favourite first"}]]
    (e/select {:options  sort-options
               :value    (opt->str sort-option)
               :onChange #(comp/transact! this [(set-root {:ui/sort-option (str->opt (evt/target-value %))})])})))

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
                                     {:label      "Sessions"
                                      :isDisabled true
                                      :iconType   "tableDensityExpanded"
                                      :onClick    #(dr/change-route! this ["session"])}]})))
      (e/horizontal-rule {:margin "none"})
      (e/flex-item {:grow false}
        (e/collapsible-nav-group {:isCollapsible false}
          (e/list-group {:size      "l"
                         :listItems [{:label      "Settings"
                                      :iconType   "gear"
                                      :isDisabled true
                                      :onClick    #(dr/change-route! this ["settings"])}]}))))))

(defonce cache (e/create-cache))

(defsc RootBody
  [this {:root/keys [router] :ui/keys [dark-mode?] :as props}]
  {:ident         (fn [] [:component/id ::root])
   :query         [[:ui/sort-option '_]
                   [:ui/dark-mode? '_]
                   {:root/router (comp/get-query RootRouter)}
                   :ui/sidebar-open?]
   :initial-state (fn [_]
                    {:root/router      (comp/get-initial-state RootRouter)
                     :ui/sidebar-open? false})}
  (e/provider {:cache cache :colorMode (if dark-mode? "dark" "light")}
    (if (get-key @store :userToken)
      (e/header {:sections [{:items [(ui-sidebar this props)] :borders "none"}
                            {:items [(ui-search-bar this props)
                                     (ui-sort-select this props)] :borders "none"}]}))
    (ui-root-router router)))

(def ui-root-body (comp/factory RootBody))

(defsc Root
  [_ {:root/keys [body-data]}]
  {:query         [{:root/body-data (comp/get-query RootBody)}]
   :initial-state (fn [_]
                    {:ui/sort-option  [{:field :bookmark/created :direction :asc}]
                     :ui/search-query {:match_all {}}
                     :ui/dark-mode?   true
                     :root/body-data  (comp/get-initial-state RootBody)})}
  (ui-root-body body-data))
