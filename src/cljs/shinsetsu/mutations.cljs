(ns shinsetsu.mutations
  (:require
    [shinsetsu.application :refer [app login-token]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.components :as comp]))

(defmutation clear-form
  "Clear a form back to its pristine stage"
  [{:keys [ident]}]
  (action
    [{:keys [state]}]
    (swap! state fs/pristine->entity* ident)))

(defmutation create-tab
  "Create a new tab"
  [_]
  (protected [_] true)
  (ok-action
    [{{{tab `create-tab} :body} :result :as env}]
    (let [TabBody (comp/registry-key->class `shinsetsu.ui.main/TabBody)
          Main    (comp/registry-key->class `shinsetsu.ui.main/Main)]
      (merge/merge-component! app TabBody tab))))

(comment
  (require '[shinsetsu.ui.main :refer [Main]])
  (comp/get-ident Main)
  (conj [:a] :b)
  (keyword `shinsetsu.ui.main/Main))
