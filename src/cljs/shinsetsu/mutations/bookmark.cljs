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
    (let [Tab (comp/registry-key->class `shinsetsu.ui.tab/Tab)]
      (merge/merge-component! app Tab bookmarks)
      (swap! state dissoc-in (conj ref :tab/password))
      (swap! state assoc-in (conj ref :ui/loading?) false)
      (swap! state assoc-in (conj ref :ui/unlocked?) true)))
  (error-action
    [{:keys [state ref] {body :body} :result}]
    (let [{:keys [error-type]} (get body `fetch-bookmarks)]
      (swap! state dissoc-in (conj ref :tab/password))
      (swap! state assoc-in (conj ref :ui/loading?) false)
      (swap! state assoc-in (conj ref :ui/error-type) error-type))))

(defmutation create-bookmark
  [{:bookmark/keys [tab-id]}]
  (action
    [{:keys [state ref]}]
    (swap! state assoc-in (conj ref :ui/loading?) true))
  (remote [_] true)
  (ok-action
    [{{{bookmark `create-bookmark} :body} :result :keys [state]}]
    (let [Bookmark       (comp/registry-key->class `shinsetsu.ui.bookmark/Bookmark)
          Tab            (comp/registry-key->class `shinsetsu.ui.tab/Tab)
          bookmark-ident (comp/get-ident Bookmark bookmark)
          tab-ident      (comp/get-ident Tab {:tab/id tab-id})]
      (merge/merge-component! app Bookmark bookmark :append (conj tab-ident :tab/bookmarks))
      (swap! state fs/entity->pristine* bookmark-ident)
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
          Tab       (comp/registry-key->class `shinsetsu.ui.tab/Tab)
          tab-ident (comp/get-ident Tab {:tab/id tab-id})]
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
    (let [Tab       (comp/registry-key->class `shinsetsu.ui.tab/Tab)
          tab-ident (comp/get-ident Tab {:tab/id tab-id})]
      (swap! state dissoc-in ref)
      (swap! state merge/remove-ident* ref (conj tab-ident :tab/bookmarks))
      (swap! state assoc-in (conj tab-ident :ui/show-bookmark-modal?) false)))
  (error-action
    [{:keys [state ref] {body :body} :result}]
    (let [{:keys [error-type]} (get body `delete-bookmark)]
      (swap! state assoc-in (conj ref :ui/loading?) false)
      (swap! state assoc-in (conj ref :ui/error-type) error-type))))
