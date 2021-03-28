(ns shinsetsu.ui.root
  (:require
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :refer [defrouter]]
    [shinsetsu.ui.login :refer [Login]]
    [shinsetsu.ui.main :refer [Main]]
    [shinsetsu.ui.user :refer [User ui-current-user]]
    [shinsetsu.ui.elastic-ui :refer [ui-button]]))

(defrouter RootRouter [_ {:keys [current-state]}]
  {:router-targets [Login Main]}
  (case current-state
    :pending (dom/div "Loading...")
    :failed (dom/div "Loading seems to have failed. Try another route.")
    (dom/div "Unknown route")))

(def ui-root-router (comp/factory RootRouter))

(defsc Root
  [_ {:root/keys    [ready? router]
      :session/keys [current-user]}]
  {:query         [:root/ready? {:root/router (comp/get-query RootRouter)}
                   {:session/current-user (comp/get-query User)}]
   :initial-state {:root/router {}}}
  (let [logged-in? (:user/valid? current-user)]
    (dom/div
      (dom/div (ui-current-user current-user))
      (when ready?
        (dom/div (ui-root-router router))))))