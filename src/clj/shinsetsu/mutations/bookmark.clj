(ns shinsetsu.mutations.bookmark
  (:require
    [shinsetsu.db.bookmark :as db]
    [com.wsscode.pathom.connect :as pc :refer [defmutation]]
    [taoensso.timbre :as log]))

;; TODO: SPEC THESE SUCKERS!

#_(defmutation create-bookmark
  [{{:user/keys [id]} :request :as env} bookmark]
  {::pc/params #{:tab/name :tab/password}
   ::pc/output [:tab/id :tab/name :tab/is-protected? :tab/created :tab/updated]}
  (log/info "User with id" id "is attempting to create a new tab")
  (-> tab
      (assoc :tab/user-id id)
      (db/create-tab)
      (assoc :tab/is-protected? ((complement nil?) (:tab/password tab)))
      (dissoc :tab/password)))
