(ns app.mutations
  (:require
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]))

(defmutation delete-person
  "Mutation: Delete the person with `:person/id` from the list with `:list/id`"
  [{list-id   :list/id
    person-id :person/id}]
  (action [{:keys [state]}]
          (swap! state merge/remove-ident* [:person/id person-id] [:list/id list-id :list/people]))
  (remote [env] true))

(defmutation login
  "Login with a username and password"
  [{:keys [username password]}]
  (action [{:keys [state]}]
          ;; TODO: Clearing should only be done when succeeded.
          (swap! state update-in [:component :login] dissoc :ui/username)
          (swap! state update-in [:component :login] dissoc :ui/password)))
