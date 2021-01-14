(ns harpocrates.mutations.storage
  (:require [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
            [com.fulcrologic.fulcro.algorithms.merge :as merge]))

(defmutation set-token
  "Mutation: Set the server-provided token into local storage."
  [token]
  (action [{:keys [state]}]
          (swap! state merge/merge* {:token token} [:global/id :token])))