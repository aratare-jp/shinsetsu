(ns shinsetsu.mutations.bookmark
  (:require
    [medley.core :refer [dissoc-in]]
    [shinsetsu.application :refer [app]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]))

(defmutation fetch-bookmarks
  [_]
  (action
    [{:keys [state ref]}]
    (swap! state assoc-in (conj ref :ui/loading?) true))
  (remote [_] true)
  (ok-action
    [{{{bookmarks `fetch-bookmarks} :body} :result :keys [state ref] :as env}]
    (let [TabBody (comp/registry-key->class `shinsetsu.ui.tab/TabBody)]
      (merge/merge-component! app TabBody bookmarks)
      (swap! state assoc-in (conj ref :ui/loading?) false)
      (swap! state assoc-in (conj ref :ui/unlocked?) true)))
  (error-action
    [{:keys [state ref] {body :body} :result}]
    (let [{:keys [error-type]} (get body `fetch-bookmarks)]
      (swap! state assoc-in (conj ref :ui/loading?) false)
      (swap! state assoc-in (conj ref :ui/error-type) error-type))))

(defmutation create-bookmark
  [{:bookmark/keys [tab-id] :as params}]
  (action
    [{:keys [state ref]}]
    (swap! state assoc-in (conj ref :ui/loading?) true))
  (remote [_] true)
  (ok-action
    [{{{bookmark `create-bookmark} :body} :result :keys [state component]}]
    (let [Bookmark       (comp/registry-key->class `shinsetsu.ui.bookmark/Bookmark)
          TabBody        (comp/registry-key->class `shinsetsu.ui.tab/TabBody)
          bookmark-ident (comp/get-ident Bookmark bookmark)
          tab-ident      (comp/get-ident TabBody {:tab/id tab-id})]
      (comp/transact! component [(fs/reset-form! {:form-ident bookmark-ident})])
      (merge/merge-component! app Bookmark bookmark :append (conj tab-ident :tab/bookmarks))
      (swap! state assoc-in (conj bookmark-ident :ui/loading?) false)
      (swap! state assoc-in (conj tab-ident :ui/show-bookmark-modal?) false)))
  (error-action
    [{:keys [state ref] {body :body} :result}]
    (let [{:keys [error-type]} (get body `create-bookmark)]
      (swap! state assoc-in (conj ref :ui/loading?) false)
      (swap! state assoc-in (conj ref :ui/error-type) error-type))))

(defmutation patch-bookmark
  [{:bookmark/keys [tab-id]}]
  (action
    [{:keys [state ref]}]
    (swap! state assoc-in (conj ref :ui/loading?) true))
  (remote [_] true)
  (ok-action
    [{{{bookmark `patch-bookmark} :body} :result :keys [state ref]}]
    (let [Bookmark  (comp/registry-key->class `shinsetsu.ui.bookmark/Bookmark)
          TabBody   (comp/registry-key->class `shinsetsu.ui.tab/TabBody)
          tab-ident (comp/get-ident TabBody {:tab/id tab-id})]
      (merge/merge-component! app Bookmark bookmark)
      (swap! state fs/entity->pristine* ref)
      (swap! state assoc-in (conj ref :ui/loading?) false)
      (swap! state assoc-in (conj tab-ident :ui/show-bookmark-modal?) false)))
  (error-action
    [{:keys [state ref] {body :body} :result}]
    (let [{:keys [error-type]} (get body `patch-bookmark)]
      (swap! state assoc-in (conj ref :ui/loading?) false)
      (swap! state assoc-in (conj ref :ui/error-type) error-type))))

(defmutation delete-bookmark
  [{:bookmark/keys [tab-id]}]
  (action
    [{:keys [state ref]}]
    (swap! state assoc-in (conj ref :ui/loading?) true))
  (remote
    [{:keys [ast]}]
    (dissoc-in ast [:params :bookmark/tab-id]))
  (ok-action
    [{:keys [ref state]}]
    (let [TabBody   (comp/registry-key->class `shinsetsu.ui.tab/TabBody)
          tab-ident (comp/get-ident TabBody {:tab/id tab-id})]
      (swap! state dissoc-in ref)
      (swap! state merge/remove-ident* ref (conj tab-ident :tab/bookmarks))
      (swap! state assoc-in (conj tab-ident :ui/show-bookmark-modal?) false)))
  (error-action
    [{:keys [state ref] {body :body} :result}]
    (let [{:keys [error-type]} (get body `delete-bookmark)]
      (swap! state assoc-in (conj ref :ui/loading?) false)
      (swap! state assoc-in (conj ref :ui/error-type) error-type))))
