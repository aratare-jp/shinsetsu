(ns shinsetsu.ui.root
  (:require
    [shinsetsu.ui.elastic :as e]
    [shinsetsu.ui.login :refer [Login]]
    [shinsetsu.ui.main :refer [Main]]
    [com.fulcrologic.fulcro.dom :refer [div h2 p]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :refer [defrouter]]
    [com.fulcrologic.fulcro.mutations :as m]))

(defrouter RootRouter
  [_ {:keys [current-state]}]
  {:router-targets [Login Main]}
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

(defsc Root
  [this {:root/keys [router] :ui/keys [dark-mode?]}]
  {:query         [:root/ready? {:root/router (comp/get-query RootRouter)} :ui/dark-mode?]
   :initial-state (fn [_] {:root/router   (comp/get-initial-state RootRouter)
                           :ui/dark-mode? true})}
  (e/provider {:colorMode (if dark-mode? "dark" "light")}
    (ui-root-router router)))
