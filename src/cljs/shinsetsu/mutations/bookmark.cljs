(ns shinsetsu.mutations.bookmark
  (:require
    [medley.core :refer [dissoc-in]]
    [shinsetsu.application :refer [app]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.algorithms.normalized-state :as ns]
    [taoensso.timbre :as log]))

(defmutation fetch-bookmarks
  [{:tab/keys [id]}]
  (action
    [{:keys [state ref]}]
    (log/debug "Fetching bookmarks for tab" id)
    (swap! state assoc-in (conj ref :ui/loading?) true))
  (remote [_] true)
  (ok-action
    [{{{bookmarks `fetch-bookmarks} :body} :result :keys [state ref]}]
    (log/debug "Bookmarks for tab" id "fetched successfully")
    (let [Tab      (comp/registry-key->class `shinsetsu.ui.tab/Tab)
          TabModal (comp/registry-key->class `shinsetsu.ui.tab/TabModal)]
      (merge/merge-component! app Tab bookmarks)
      (swap! state #(-> %
                        (fs/add-form-config* TabModal ref)
                        (dissoc-in (conj ref :ui/password))
                        (assoc-in (conj ref :ui/loading?) false)
                        (assoc-in (conj ref :ui/unlocked?) true)))))
  (error-action
    [{{{{:keys [error-type error-message]} `fetch-bookmarks} :body} :result :keys [state ref]}]
    (log/error "Failed to fetch bookmarks due to:" error-message)
    (swap! state #(-> %
                      (dissoc-in (conj ref :ui/password))
                      (assoc-in (conj ref :ui/loading?) false)
                      (assoc-in (conj ref :ui/error-type) error-type)))))

(defmutation create-bookmark
  [{:bookmark/keys [tab-id]}]
  (action
    [{:keys [state ref]}]
    (log/debug "Creating new bookmark in tab" tab-id)
    (swap! state assoc-in (conj ref :ui/loading?) true))
  (remote [_] true)
  (ok-action
    [{{{{:bookmark/keys [id] :as bookmark} `create-bookmark} :body} :result :keys [state]}]
    (log/debug "Bookmark" id "created for tab" tab-id "successfully")
    (let [Bookmark       (comp/registry-key->class `shinsetsu.ui.bookmark/Bookmark)
          Tab            (comp/registry-key->class `shinsetsu.ui.tab/Tab)
          bookmark-ident (comp/get-ident Bookmark bookmark)
          tab-ident      (comp/get-ident Tab {:tab/id tab-id})]
      (merge/merge-component! app Bookmark bookmark :append (conj tab-ident :tab/bookmarks))
      (swap! state #(-> %
                        (fs/entity->pristine* bookmark-ident)
                        (assoc-in (conj bookmark-ident :ui/loading?) false)
                        (assoc-in (conj tab-ident :ui/show-bookmark-modal?) false)))))
  (error-action
    [{{{{:keys [error-type error-message]} `create-bookmark} :body} :result :keys [ref state]}]
    (log/error "Create bookmark failed due to:" error-message)
    (swap! state #(-> %
                      (assoc-in (conj ref :ui/loading?) false)
                      (assoc-in (conj ref :ui/error-type) error-type)))))

(defmutation patch-bookmark
  [{:bookmark/keys [id tab-id]}]
  (action
    [{:keys [state ref]}]
    (log/debug "Patching bookmark" id)
    (swap! state assoc-in (conj ref :ui/loading?) true))
  (remote [_] true)
  (ok-action
    [{{{bookmark `patch-bookmark} :body} :result :keys [state ref]}]
    (log/debug "Bookmark" id "patched successfully")
    (let [Bookmark  (comp/registry-key->class `shinsetsu.ui.bookmark/Bookmark)
          Tab       (comp/registry-key->class `shinsetsu.ui.tab/Tab)
          tab-ident (comp/get-ident Tab {:tab/id tab-id})]
      (merge/merge-component! app Bookmark bookmark)
      (swap! state #(-> %
                        (fs/entity->pristine* ref)
                        (assoc-in (conj ref :ui/loading?) false)
                        (assoc-in (conj tab-ident :ui/show-bookmark-modal?) false)))))
  (error-action
    [{{{{:keys [error-type error-message]} `patch-bookmark} :body} :result :keys [ref state]}]
    (log/error "Failed to patch bookmark" id "due to:" error-message)
    (swap! state #(-> %
                      (assoc-in (conj ref :ui/loading?) false)
                      (assoc-in (conj ref :ui/error-type) error-type)))))

(defmutation delete-bookmark
  [{:bookmark/keys [id tab-id]}]
  (action
    [{:keys [state ref]}]
    (log/debug "Deleting bookmark" id)
    (swap! state assoc-in (conj ref :ui/loading?) true))
  (remote
    [{:keys [ast]}]
    (dissoc-in ast [:params :bookmark/tab-id]))
  (ok-action
    [{:keys [ref state]}]
    (log/debug "Bookmark" id "deleted successfully")
    (let [Tab       (comp/registry-key->class `shinsetsu.ui.tab/Tab)
          tab-ident (comp/get-ident Tab {:tab/id tab-id})]
      #_(swap! state dissoc-in ref)
      #_(swap! state merge/remove-ident* ref (conj tab-ident :tab/bookmarks))
      (swap! state #(-> %
                        (ns/remove-entity ref)
                        (assoc-in (conj tab-ident :ui/show-bookmark-modal?) false)))))
  (error-action
    [{{{{:keys [error-type error-message]} `delete-bookmark} :body} :result :keys [ref state]}]
    (log/error "Failed to delete bookmark" id "due to" error-message)
    (swap! state #(-> %
                      (assoc-in (conj ref :ui/loading?) false)
                      (assoc-in (conj ref :ui/error-type) error-type)))))
