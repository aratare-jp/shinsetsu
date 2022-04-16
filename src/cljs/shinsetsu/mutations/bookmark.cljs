(ns shinsetsu.mutations.bookmark
  (:require
    [medley.core :refer [dissoc-in]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.algorithms.normalized-state :as ns]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]))

(defmutation create-bookmark
  [{:bookmark/keys [tab-id]}]
  (action
    [{:keys [state ref]}]
    (log/debug "Creating new bookmark in tab" tab-id)
    (swap! state assoc-in (conj ref :ui/loading?) true))
  (remote [_] true)
  (ok-action
    [{{{{:bookmark/keys [id] :as bookmark} `create-bookmark} :body} :result :keys [state component]}]
    (log/debug "Bookmark" id "created for tab" tab-id "successfully")
    (let [Bookmark       (comp/registry-key->class `shinsetsu.ui.bookmark/Bookmark)
          Tab            (comp/registry-key->class `shinsetsu.ui.tab/Tab)
          bookmark-ident (comp/get-ident Bookmark bookmark)
          tab-ident      (comp/get-ident Tab {:tab/id tab-id})]
      (swap! state #(-> %
                        (merge/merge-component component bookmark)
                        (dissoc-in (conj tab-ident :ui/edit-bm-id))
                        (assoc-in (conj bookmark-ident :ui/loading?) false)
                        (fs/entity->pristine* bookmark-ident)))))
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
    (let [Tab            (comp/registry-key->class `shinsetsu.ui.tab/Tab)
          target-tab-idt (comp/get-ident Tab {:tab/id tab-id})
          Bookmark       (comp/registry-key->class `shinsetsu.ui.bookmark/Bookmark)
          bm-idt         (comp/get-ident Bookmark bookmark)]
      (swap! state #(-> %
                        (merge/merge-component Bookmark bookmark)
                        (dissoc-in (conj target-tab-idt :ui/edit-bm-id))
                        (dissoc-in (conj ref :ui/move-bm-id))
                        (assoc-in (conj ref :ui/loading?) false)
                        (fs/entity->pristine* ref)))
      ;; Check if this mutation is fired by a move op or not.
      (if (= (first ref) :tab/id)
        (swap! state #(-> %
                          (targeting/integrate-ident* bm-idt :append (conj ref :tab/bookmarks))
                          (merge/remove-ident* bm-idt (conj ref :tab/bookmarks)))))))
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
      (swap! state #(-> %
                        (ns/remove-entity [:bookmark/id id])
                        (dissoc-in (conj tab-ident :ui/delete-bm-id))
                        (assoc-in (conj tab-ident :ui/loading?) false)))))
  (error-action
    [{{{{:keys [error-type error-message]} `delete-bookmark} :body} :result :keys [ref state]}]
    (log/error "Failed to delete bookmark" id "due to" error-message)
    (swap! state #(-> %
                      (assoc-in (conj ref :ui/loading?) false)
                      (assoc-in (conj ref :ui/error-type) error-type)))))
