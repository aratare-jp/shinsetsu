(ns shinsetsu.mutations.bookmark
  (:require
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
  "Create a new tab"
  [{:bookmark/keys [tab-id]}]
  (action
    [{:keys [state ref]}]
    (swap! state assoc-in (conj ref :ui/loading?) true))
  (remote [_] true)
  (ok-action
    [{{{bookmark `create-bookmark} :body} :result :keys [state ref component] :as env}]
    (let [Bookmark      (comp/registry-key->class `shinsetsu.ui.bookmark/Bookmark)
          BookmarkModal (comp/registry-key->class `shinsetsu.ui.bookmark/BookmarkModal)
          TabBody       (comp/registry-key->class `shinsetsu.ui.tab/TabBody)]
      (comp/transact! component [(fs/reset-form! {:form-ident (comp/get-ident component)})])
      (merge/merge-component! app Bookmark bookmark :append (conj (comp/get-ident TabBody {:tab/id tab-id}) :tab/bookmarks))
      (swap! state assoc-in (conj (comp/get-ident BookmarkModal bookmark) :ui/loading?) false)
      (swap! state assoc-in (conj (comp/get-ident TabBody {:tab/id tab-id}) :ui/show-bookmark-modal?) false)))
  (error-action
    [{:keys [state ref] {body :body} :result}]
    (let [{:keys [error-type]} (get body `create-bookmark)]
      (swap! state assoc-in (conj ref :ui/loading?) false)
      (swap! state assoc-in (conj ref :ui/error-type) error-type))))
