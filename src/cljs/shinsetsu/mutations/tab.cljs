(ns shinsetsu.mutations.tab
  (:require
    [shinsetsu.application :refer [app login-token]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.mutations :as m]))

(defmutation create-tab
  "Create a new tab"
  [_]
  (remote [_] true)
  (ok-action
    [{{{tab `create-tab} :body} :result :keys [state ref component] :as env}]
    (js/console.log env)
    (let [TabBody (comp/registry-key->class `shinsetsu.ui.tab/TabBody)
          Main    (comp/registry-key->class `shinsetsu.ui.main/Main)]
      (comp/transact! component [(fs/reset-form! {:form-ident (comp/get-ident component)})])
      (merge/merge-component! app TabBody tab :append (conj (comp/get-ident Main {}) :user/tabs))
      (swap! state assoc-in (conj (comp/get-ident Main {}) :ui/loading?) false)
      (swap! state assoc-in (conj (comp/get-ident Main {}) :ui/show-modal?) false)))
  (error-action
    [{:keys [state ref] {body :body} :result}]
    (let [{:keys [error-type]} (get body `create-tab)]
      (swap! state assoc-in (conj ref :ui/loading?) false)
      (swap! state assoc-in (conj ref :ui/error-type) error-type))))
