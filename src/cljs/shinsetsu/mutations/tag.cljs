(ns shinsetsu.mutations.tag
  (:require
    [medley.core :refer [dissoc-in]]
    [shinsetsu.application :refer [app]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.mutations :as m]
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
      (merge/merge-component! app component tag :append [:root/tags])
      (swap! state #(-> %
                        (fs/entity->pristine* tag-ident)
                        (assoc-in (conj tag-ident :ui/loading?) false)
                        (assoc-in (conj main-ident :ui/show-create-modal?) false)))))
  (error-action
    [{{{{:keys [error-type error-message]} `create-tag} :body} :result :keys [state ref]}]
    (log/error "Failed to create tag due to:" error-message)
    (swap! state #(-> %
                      (assoc-in (conj ref :ui/loading?) false)
                      (assoc-in (conj ref :ui/error-type) error-type)))))

(defmutation patch-tag
  [{:tag/keys [id password]}]
  (action
    [{:keys [state ref]}]
    (log/debug "Patching tag" id)
    (swap! state assoc-in (conj ref :ui/loading?) true))
  (remote
    [{:keys [ast]}]
    (if (nil? password)
      (dissoc-in ast [:params :tag/password])
      ast))
  (ok-action
    [{{{tag `patch-tag} :body} :result :keys [state ref component]}]
    (log/debug "tag" id "patched successfully")
    (let [TagMain  (comp/registry-key->class `shinsetsu.ui.tag/TagMain)
          main-idt (comp/get-ident TagMain {})]
      (merge/merge-component! app component tag)
      (swap! state #(-> %
                        (fs/entity->pristine* ref)
                        (dissoc-in (conj ref :ui/password))
                        (assoc-in (conj ref :ui/loading?) false)
                        (assoc-in (conj main-idt :ui/show-edit-modal?) false)))))
  (error-action
    [{{{{:keys [error-type error-message]} `patch-tag} :body} :result :keys [state ref]}]
    (log/error "Failed to patch tag" id "due to:" error-message)
    (swap! state #(-> %
                      (assoc-in (conj ref :ui/loading?) false)
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
                        (assoc-in (conj ref :ui/selected-idx) 0)
                        (assoc-in (conj ref :ui/loading?) false)
                        (assoc-in (conj ref :ui/show-delete-modal?) false)
                        (ns/remove-entity tag-idt #{:tag/bookmarks})))))
  (error-action
    [{{{{:keys [error-message error-type]} `delete-tag} :body} :result :keys [state ref]}]
    (log/error "Failed to delete tag" id "due to:" error-message)
    (swap! state #(-> %
                      (assoc-in (conj ref :ui/loading?) false)
                      (assoc-in (conj ref :ui/error-type) error-type)))))

(defmutation lock-tag
  [p]
  (action
    [{:keys [state]}]
    (log/debug "Locking up tag" (:tag/id p))
    (let [Tag (comp/registry-key->class `shinsetsu.ui.tag/TagModal)]
      (swap! state assoc-in (conj (comp/get-ident Tag p) :ui/unlocked?) false))))
