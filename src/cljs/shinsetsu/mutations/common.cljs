(ns shinsetsu.mutations.common
  (:require
    [medley.core :refer [dissoc-in]]
    [com.fulcrologic.fulcro.algorithms.merge :refer [remove-ident*]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.algorithms.normalized-state :as ns]))

(defmutation remove-ident
  [{:keys [ident]}]
  (action
    [{:keys [state]}]
    (swap! state ns/remove-entity ident)))

(defmutation set-root
  [m]
  (action
    [{:keys [state]}]
    (swap! state merge m)))
