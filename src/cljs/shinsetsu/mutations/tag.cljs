(ns shinsetsu.mutations.tag
  (:require
    [medley.core :refer [dissoc-in]]
    [shinsetsu.application :refer [app]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.components :as comp]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.algorithms.normalized-state :as ns]))

(defmutation create-tag
  [_]
  (action
    [{:keys [state ref]}]
    (log/debug "Creating new tag")
    (swap! state assoc-in (conj ref :ui/loading?) true))
  (remote [_] true)
  (ok-action
    [{{{{:tag/keys [id] :as tag} `create-tag} :body} :result :keys [state component]}]
    (log/debug "Tag" id "was created successfully")
    (let [TagMain    (comp/registry-key->class `shinsetsu.ui.tag/TagMain)
          main-ident (comp/get-ident TagMain {})
          tag-ident  (comp/get-ident component tag)]
      (swap! state #(-> %
                        (merge/merge-component component tag)
                        (assoc-in (conj tag-ident :ui/loading?) false)
                        (assoc-in (conj main-ident :ui/show-create-modal?) false)
                        (fs/entity->pristine* tag-ident)))))
  (error-action
    [{{{{:keys [error-type error-message]} `create-tag} :body} :result :keys [state ref]}]
    (log/error "Failed to create tag due to:" error-message)
    (swap! state #(-> %
                      (assoc-in (conj ref :ui/loading?) false)
                      (assoc-in (conj ref :ui/error-type) error-type)))))

(defmutation patch-tag
  [{:tag/keys [id]}]
  (action
    [{:keys [state ref]}]
    (log/debug "Patching tag" id)
    (swap! state assoc-in (conj ref :ui/loading?) true))
  (remote [_] true)
  (ok-action
    [{{{tag `patch-tag} :body} :result :keys [state ref component]}]
    (log/debug "Tag" id "patched successfully")
    (let [TagMain  (comp/registry-key->class `shinsetsu.ui.tag/TagMain)
          main-idt (comp/get-ident TagMain {})]
      (swap! state #(-> %
                        (merge/merge-component component tag)
                        (assoc-in (conj ref :ui/loading?) false)
                        (dissoc-in (conj main-idt :ui/edit-tag-id))
                        (fs/entity->pristine* ref)))))
  (error-action
    [{{{{:keys [error-type error-message]} `patch-tag} :body} :result :keys [state ref]}]
    (log/error "Failed to patch tag" id "due to:" error-message)
    (swap! state #(-> %
                      (assoc-in (conj ref :ui/loading?) false)
                      (assoc-in (conj ref :ui/error-type) error-type)))))

(defmutation fetch-tags
  [_]
  (action
    [{:keys [state ref]}]
    (log/debug "Fetching all tags")
    (swap! state assoc-in (conj ref :ui/loading?) true))
  (remote [_] true)
  (ok-action
    [{{{{:user/keys [tags]} `fetch-tags} :body} :result :keys [state ref component]}]
    (log/debug "Tags fetched patched successfully")
    (swap! state #(-> %
                      (merge/merge-component component {:ui/tags tags})
                      (assoc-in (conj ref :ui/loading?) false))))
  (error-action
    [{{{{:keys [error-type error-message]} `fetch-tags} :body} :result :keys [state ref]}]
    (log/error "Failed to fetch tags due to:" error-message)
    (swap! state #(-> %
                      (assoc-in (conj ref :ui/loading?) false)
                      (assoc-in (conj ref :ui/error-type) error-type)))))

(defmutation fetch-tag-options
  [_]
  (action
    [{:keys [state ref]}]
    (log/debug "Fetching tag options")
    (swap! state assoc-in (conj ref :ui/tags-loading?) true))
  (remote
    [{:keys [ast]}]
    (assoc ast :key `fetch-tags))
  (ok-action
    [{{{{:user/keys [tags]} `fetch-tags} :body} :result :keys [state ref]}]
    (log/debug "Tag options fetched patched successfully")
    (swap! state #(-> %
                      (assoc-in (conj ref :ui/tag-options) tags)
                      (assoc-in (conj ref :ui/tags-loading?) false))))
  (error-action
    [{{{{:keys [error-type error-message]} `fetch-tag-options} :body} :result :keys [state ref]}]
    (log/error "Failed to fetch tag options due to:" error-message)
    (swap! state #(-> %
                      (assoc-in (conj ref :ui/tags-loading?) false)
                      (assoc-in (conj ref :ui/error-type) error-type)))))

(defmutation delete-tag
  [{:tag/keys [id]}]
  (action
    [{:keys [state ref]}]
    (log/debug "Deleting tag" id)
    (swap! state assoc-in (conj ref :ui/loading?) true))
  (remote [_] true)
  (ok-action
    [{{{tag `delete-tag} :body} :result :keys [state ref]}]
    (log/debug "tag" id "deleted successfully")
    (let [Tag     (comp/registry-key->class `shinsetsu.ui.tag/TagModal)
          tag-idt (comp/get-ident Tag tag)]
      (swap! state #(-> %
                        (assoc-in (conj ref :ui/current-tag-idx) 0)
                        (assoc-in (conj ref :ui/loading?) false)
                        (assoc-in (conj ref :ui/show-delete-modal?) false)
                        (ns/remove-entity tag-idt #{:tag/bookmarks})))))
  (error-action
    [{{{{:keys [error-message error-type]} `delete-tag} :body} :result :keys [state ref]}]
    (log/error "Failed to delete tag" id "due to:" error-message)
    (swap! state #(-> %
                      (assoc-in (conj ref :ui/loading?) false)
                      (assoc-in (conj ref :ui/error-type) error-type)))))
