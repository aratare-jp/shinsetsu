(ns shinsetsu.mutations.tab
  (:require
    [medley.core :refer [dissoc-in]]
    [shinsetsu.application :refer [app]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.mutations :as m]))

(defmutation create-tab
  [_]
  (action
    [{:keys [state ref]}]
    (swap! state assoc-in (conj ref :ui/loading?) true))
  (remote [_] true)
  (ok-action
    [{{{tab `create-tab} :body} :result :keys [state ref component] :as env}]
    (let [Tab       (comp/registry-key->class `shinsetsu.ui.tab/Tab)
          TabModal  (comp/registry-key->class `shinsetsu.ui.tab/TabModal)
          Main      (comp/registry-key->class `shinsetsu.ui.main/Main)
          tab-ident (comp/get-ident TabModal tab)]
      (comp/transact! component [(fs/reset-form! {:form-ident (comp/get-ident component)})])
      (merge/merge-component! app Tab tab :append (conj (comp/get-ident Main {}) :user/tabs))
      (swap! state dissoc-in (conj tab-ident :tab/password))
      (swap! state assoc-in (conj tab-ident :ui/loading?) false)
      (swap! state assoc-in (conj (comp/get-ident Main {}) :ui/show-tab-modal?) false)))
  (error-action
    [{:keys [state ref] {body :body} :result}]
    (let [{:keys [error-type]} (get body `create-tab)]
      (swap! state assoc-in (conj ref :ui/loading?) false)
      (swap! state assoc-in (conj ref :ui/error-type) error-type))))

(defmutation delete-tab
  [_]
  (remote [_] true)
  (ok-action
    [{:keys [state ref]}]
    (let [Main (comp/registry-key->class `shinsetsu.ui.main/Main)]
      (doseq [bm (-> @state (get-in ref) :tab/bookmarks)]
        (swap! state dissoc-in bm))
      (swap! state dissoc-in ref)
      (swap! state merge/remove-ident* ref (conj (comp/get-ident Main {}) :user/tabs))))
  (error-action
    [{:keys [state ref] {body :body} :result}]
    (let [{:keys [error-type]} (get body `create-tab)]
      (swap! state assoc-in (conj ref :ui/error-type) error-type))))
