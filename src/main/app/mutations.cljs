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

(def login-token (atom nil))

(defmutation login
  "Login with a username and password"
  [{:keys [username password]}]
  (remote [env] true)
  (ok-action [{:keys [result] :as env}] (swap! login-token #(-> result (get :body) vals first)))
  (error-action [env] (js/alert "Oops seems like your credentials are not correct.")))
