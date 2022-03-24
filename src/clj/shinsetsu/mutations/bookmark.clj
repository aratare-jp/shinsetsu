(ns shinsetsu.mutations.bookmark
  (:require
    [shinsetsu.db.bookmark :as db]
    [com.wsscode.pathom.connect :as pc :refer [defmutation]]
    [taoensso.timbre :as log]
    [malli.core :as m]
    [malli.error :as me]
    [shinsetsu.schema :as s]))

(def bookmark-input #{:bookmark/title :bookmark/url :bookmark/image :bookmark/user-id :bookmark/tab-id})
(def bookmark-output [:bookmark/id :bookmark/title :bookmark/url :bookmark/image :bookmark/created :bookmark/updated])

;; BOOKMARK

(defmutation create-bookmark
  [{{user-id :user/id} :request} bookmark]
  {::pc/params bookmark-input
   ::pc/output bookmark-output}
  (let [bookmark (assoc bookmark :bookmark/user-id user-id)]
    (if-let [err (m/explain s/bookmark-create-spec bookmark)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
      (do
        (log/info "User with id" user-id "is attempting to create a new bookmark")
        (let [bookmark    (db/create-bookmark bookmark)
              bookmark-id (:bookmark/id bookmark)]
          (log/info "User with ID" user-id "created bookmark" bookmark-id "successfully")
          bookmark)))))

(defmutation patch-bookmark
  [{{user-id :user/id} :request} {:bookmark/keys [id] :as bookmark}]
  {::pc/params bookmark-input
   ::pc/output bookmark-output}
  (let [bookmark (assoc bookmark :bookmark/user-id user-id)]
    (if-let [err (m/explain s/bookmark-patch-spec bookmark)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
      (do
        (log/info "User with id" user-id "is attempting to patch bookmark" id)
        (let [bookmark (db/patch-bookmark bookmark)]
          (log/info "User with ID" user-id "patched bookmark" id "successfully")
          bookmark)))))

(defmutation delete-bookmark
  [{{user-id :user/id} :request} {:bookmark/keys [id] :as bookmark}]
  {::pc/params #{:bookmark/id}
   ::pc/output bookmark-output}
  (let [bookmark (assoc bookmark :bookmark/user-id user-id)]
    (if-let [err (m/explain s/bookmark-delete-spec bookmark)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
      (do
        (log/info "User with id" user-id "is attempting to delete bookmark" id)
        (let [bookmark (db/delete-bookmark bookmark)]
          (log/info "User with ID" user-id "deleted bookmark" id "successfully")
          bookmark)))))
