(ns shinsetsu.mutations.common
  (:require
    [medley.core :refer [dissoc-in]]
    [com.fulcrologic.fulcro.algorithms.merge :refer [remove-ident*]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]))

(defmutation remove-ident
  [{:keys [ident remove-from]}]
  (action
    [{:keys [state]}]
    (swap! state dissoc-in ident)
    (if remove-from
      (swap! state remove-ident* ident remove-from))))
