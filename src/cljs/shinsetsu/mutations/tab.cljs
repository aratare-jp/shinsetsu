(ns shinsetsu.mutations.tab
  (:require
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.algorithms.normalized-state :as ns]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [medley.core :refer [dissoc-in]]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.data-fetch :as df]))

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
    (let [TabMain    (comp/registry-key->class `shinsetsu.ui.tab/TabMain)
          main-ident (comp/get-ident TabMain {})
          tab-ident  (comp/get-ident component tab)]
      (swap! state #(-> %
                        (merge/merge-component component tab)
                        (dissoc-in (conj tab-ident :ui/password))
                        (assoc-in (conj tab-ident :ui/loading?) false)
                        (assoc-in (conj main-ident :ui/edit-tab-id) nil)
                        (fs/entity->pristine* tab-ident)))))
  (error-action
    [{{{{:keys [error-type error-message]} `create-tab} :body} :result :keys [state ref]}]
    (log/error "Failed to create tab due to:" error-message)
    (swap! state #(-> %
                      (assoc-in (conj ref :ui/loading?) false)
                      (assoc-in (conj ref :ui/error-type) error-type)))))

(defmutation patch-tab
  [{:tab/keys [id change-password?]}]
  (action
    [{:keys [state ref]}]
    (log/debug "Patching tab" id)
    (swap! state assoc-in (conj ref :ui/loading?) true))
  (remote
    [{:keys [ast]}]
    (let [ast (dissoc-in ast [:params :tab/change-password?])]
      (if change-password?
        ast
        (dissoc-in ast [:params :tab/password]))))
  (ok-action
    [{{{tab `patch-tab} :body} :result :keys [state ref component]}]
    (log/debug "Tab" id "patched successfully")
    (let [TabMain  (comp/registry-key->class `shinsetsu.ui.tab/TabMain)
          main-idt (comp/get-ident TabMain {})]
      (swap! state #(-> %
                        (merge/merge-component component tab)
                        (dissoc-in (conj ref :ui/password))
                        (assoc-in (conj ref :ui/change-password?) false)
                        (assoc-in (conj ref :ui/loading?) false)
                        (dissoc-in (conj main-idt :ui/edit-tab-id))
                        (fs/entity->pristine* ref)))))
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
    [{{{tab `delete-tab} :body} :result :keys [state ref]}]
    (log/debug "Tab" id "deleted successfully")
    (let [Tab     (comp/registry-key->class `shinsetsu.ui.tab/Tab)
          tab-idt (comp/get-ident Tab tab)]
      (swap! state #(-> %
                        (assoc-in (conj ref :ui/current-tab-idx) 0)
                        (assoc-in (conj ref :ui/loading?) false)
                        (dissoc-in (conj ref :ui/delete-tab-id))
                        (ns/remove-entity tab-idt #{:tab/bookmarks})))))
  (error-action
    [{{{{:keys [error-message error-type]} `delete-tab} :body} :result :keys [state ref]}]
    (log/error "Failed to delete tab" id "due to:" error-message)
    (swap! state #(-> %
                      (assoc-in (conj ref :ui/loading?) false)
                      (assoc-in (conj ref :ui/error-type) error-type)))))

(defmutation post-load-query-bookmarks
  [p]
  (action
    [{:keys [state]}]
    (log/debug "Post queried bookmarks")
    (let [TabMain (comp/registry-key->class `shinsetsu.ui.tab/TabMain)
          tab-idt (comp/get-ident TabMain {})]
      (swap! state #(-> %
                        (assoc-in (conj tab-idt :ui/current-tab-idx) 0)
                        (assoc-in (conj tab-idt :ui/loading?) false)
                        (assoc-in (conj tab-idt :ui/unlocked?) true))))))

(defmutation post-load-locked-bookmarks
  [p]
  (action
    [{:keys [state]}]
    (log/debug "Post load protected bookmarks for tab" (:tab/id p))
    (let [Tab     (comp/registry-key->class `shinsetsu.ui.tab/Tab)
          tab-idt (comp/get-ident Tab p)]
      (swap! state #(-> %
                        (assoc-in (conj tab-idt :ui/loading?) false)
                        (assoc-in (conj tab-idt :ui/unlocked?) true))))))

(defmutation post-load-bookmarks-error
  [{{:keys [source-key]} :load-params :keys [result]}]
  (action
    [{:keys [state]}]
    (swap! state #(-> %
                      (assoc-in (conj source-key :ui/error-type) (get-in result [:body source-key :error-type]))
                      (assoc-in (conj source-key :ui/loading?) false)))))

(defmutation post-query-bookmarks-error
  [{{:keys [source-key]} :load-params :keys [result]}]
  (action
    [{:keys [state]}]
    (swap! state #(-> %
                      (assoc-in (conj source-key :ui/search-error-type) (get-in result [:body source-key :error-type]))
                      (assoc-in (conj source-key :ui/loading?) false)))))

(defmutation post-load-unlocked-bookmarks
  [p]
  (action
    [{:keys [state]}]
    (log/debug "Post load unprotected bookmarks for tab" (:tab/id p))
    (let [Tab     (comp/registry-key->class `shinsetsu.ui.tab/Tab)
          tab-idt (comp/get-ident Tab p)]
      (swap! state assoc-in (conj tab-idt :ui/loading?) false))))

(defmutation lock-tab
  [p]
  (action
    [{:keys [state]}]
    (log/debug "Locking up tab" (:tab/id p))
    (let [Tab     (comp/registry-key->class `shinsetsu.ui.tab/Tab)
          tab-idt (comp/get-ident Tab p)]
      (swap! state assoc-in (conj tab-idt :ui/unlocked?) false)
      (swap! state #(let [tid (get-in % (conj tab-idt :ui/load-timer))]
                      (js/clearInterval tid)
                      (dissoc-in % (conj tab-idt :ui/load-timer)))))))
