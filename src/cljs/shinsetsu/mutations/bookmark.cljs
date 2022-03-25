(ns shinsetsu.mutations.bookmark
  (:require
    [shinsetsu.application :refer [app]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]))

(defmutation fetch-bookmarks
  [{:tab/keys [id]}]
  (remote [_] true)
  (ok-action
    [{{{bookmarks `fetch-bookmarks} :body} :result :keys [state ref component] :as env}]
    (let [TabBody (comp/registry-key->class `shinsetsu.ui.tab/TabBody)]
      (merge/merge-component! app TabBody bookmarks)
      (swap! state assoc-in (conj ref :ui/loading?) false)))
  (error-action
    [{:keys [state ref] {body :body} :result}]
    (let [{:keys [error-type]} (get body `fetch-bookmarks)]
      (swap! state assoc-in (conj ref :ui/loading?) false)
      (swap! state assoc-in (conj ref :ui/error-type) error-type))))
