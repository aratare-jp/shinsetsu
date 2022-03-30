(ns shinsetsu.mutations.tab
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

(defmutation create-tab
  [{:tab/keys [password]}]
  (action
    [{:keys [state ref]}]
    (log/debug "Creating new tab")
    (swap! state assoc-in (conj ref :ui/loading?) true))
  (remote
    [{:keys [ast]}]
    (if (not-empty password)
      ast
      (dissoc-in ast [:params :tab/password])))
  (ok-action
    [{{{{:tab/keys [id] :as tab} `create-tab} :body} :result :keys [state component]}]
    (log/debug "Tab" id "was created successfully")
    (let [Main       (comp/registry-key->class `shinsetsu.ui.main/Main)
          main-ident (comp/get-ident Main {})
          tab-ident  (comp/get-ident component tab)]
      (merge/merge-component! app component tab :append (conj main-ident :user/tabs))
      (swap! state #(-> %
                        (fs/entity->pristine* tab-ident)
                        (dissoc-in (conj tab-ident :ui/password))
                        (assoc-in (conj tab-ident :ui/loading?) false)
                        (assoc-in (conj main-ident :ui/show-tab-modal?) false)))))
  (error-action
    [{{{{:keys [error-type error-message]} `create-tab} :body} :result :keys [state ref]}]
    (log/error "Failed to create tab due to:" error-message)
    (swap! state #(-> %
                      (assoc-in (conj ref :ui/loading?) false)
                      (assoc-in (conj ref :ui/error-type) error-type)))))

(defmutation patch-tab
  [{:tab/keys [id password]}]
  (action
    [{:keys [state ref]}]
    (log/debug "Patching tab" id)
    (swap! state assoc-in (conj ref :ui/loading?) true))
  (remote
    [{:keys [ast]}]
    (if (nil? password)
      (dissoc-in ast [:params :tab/password])
      ast))
  (ok-action
    [{{{tab `patch-tab} :body} :result :keys [state ref component]}]
    (log/debug "Tab" id "patched successfully")
    (merge/merge-component! app component tab)
    (swap! state #(-> %
                      (fs/entity->pristine* ref)
                      (dissoc-in (conj ref :ui/password))
                      (assoc-in (conj ref :ui/loading?) false)
                      (assoc-in (conj ref :ui/show-tab-modal?) false))))
  (error-action
    [{{{{:keys [error-type error-message]} `patch-tab} :body} :result :keys [state ref]}]
    (log/error "Failed to patch tab" id "due to:" error-message)
    (swap! state #(-> %
                      (assoc-in (conj ref :ui/loading?) false)
                      (assoc-in (conj ref :ui/error-type) error-type)))))

(defmutation delete-tab
  [{:tab/keys [id]}]
  (action
    [{:keys [state ref]}]
    (log/debug "Deleting tab" id)
    (swap! state assoc-in (conj ref :ui/loading?) true))
  (remote [_] true)
  (ok-action
    [{:keys [state ref]}]
    (log/debug "Tab" id "deleted successfully")
    (swap! state ns/remove-entity ref #{:tab/bookmarks}))
  (error-action
    [{{{{:keys [error-message error-type]} `delete-tab} :body} :result :keys [state ref]}]
    (log/error "Failed to delete tab" id "due to:" error-message)
    (swap! state #(-> %
                      (assoc-in (conj ref :ui/loading?) false)
                      (assoc-in (conj ref :ui/error-type) error-type)))))
