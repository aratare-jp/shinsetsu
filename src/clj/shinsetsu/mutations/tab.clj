(ns shinsetsu.mutations.tab
  (:require
    [shinsetsu.db.tab :as db]
    [com.wsscode.pathom.connect :as pc :refer [defmutation]]
    [taoensso.timbre :as log]))

;; TODO: SPEC THESE SUCKERS!

(defmutation create-tab
  [{{:user/keys [id]} :request :as env} tab]
  {::pc/params #{:tab/name :tab/password}
   ::pc/output [:tab/id :tab/name :tab/is-protected? :tab/created :tab/updated]}
  (log/info "User with id" id "is attempting to create a new tab")
  (try
    (-> tab
        (assoc :tab/user-id id)
        (db/create-tab)
        (assoc :tab/is-protected? ((complement nil?) (:tab/password tab)))
        (dissoc :tab/password))
    (catch Exception e (throw (ex-info "Error while creating tab" {} e)))))
