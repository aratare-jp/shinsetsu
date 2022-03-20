(ns shinsetsu.mutations.bookmark
  (:require
    [shinsetsu.db.bookmark :as db]
    [com.wsscode.pathom.connect :as pc :refer [defmutation]]
    [taoensso.timbre :as log]
    [malli.core :as m]
    [malli.error :as me]
    [shinsetsu.schema :as s]))

;; TODO: SPEC THESE SUCKERS!

(defmutation create-bookmark
  [{{user-id :user/id} :request :as env} bookmark]
  {::pc/params #{:bookmark/title :bookmark/url :bookmark/image :bookmark/user-id :bookmark/tab-id}
   ::pc/output [:bookmark/id :bookmark/title :bookmark/url :bookmark/image :bookmark/created :bookmark/updated]}
  (let [bookmark (assoc bookmark :user/id user-id)]
    (if-let [err (m/explain s/bookmark-spec bookmark)]
      (throw (ex-info "Invalid bookmark" {:error-type :invalid-input :error-data (me/humanize err)}))
      (do
        (log/info "User with id" user-id "is attempting to create a new bookmark")
        (db/create-bookmark bookmark)))))
