(ns shinsetsu.mutations.bookmark
  (:require
    [shinsetsu.db.bookmark :as db]
    [com.wsscode.pathom.connect :as pc :refer [defmutation]]
    [taoensso.timbre :as log]
    [malli.core :as m]
    [malli.error :as me]
    [shinsetsu.schema :as s]
    [shinsetsu.db.tab :as tab-db]
    [buddy.hashers :as hashers]))

(def bookmark-input #{:bookmark/title :bookmark/url :bookmark/image :bookmark/user-id :bookmark/tab-id})
(def bookmark-output [:bookmark/id :bookmark/title :bookmark/url :bookmark/image :bookmark/created :bookmark/updated])

(defmutation create-bookmark
  [{{user-id :user/id} :request} bookmark]
  {::pc/params bookmark-input
   ::pc/output bookmark-output}
  (let [bookmark (assoc bookmark :bookmark/user-id user-id)]
    (if-let [err (m/explain s/bookmark-create-spec bookmark)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
      (do
        (log/info "User with id" user-id "is attempting to create a new bookmark")
        (let [tempid   (:bookmark/id bookmark)
              bookmark (-> bookmark (dissoc :bookmark/id) db/create-bookmark)
              real-id  (:bookmark/id bookmark)]
          (log/info "User with ID" user-id "created bookmark" real-id "successfully")
          (merge bookmark {:tempids {tempid real-id}}))))))

(defmutation fetch-bookmarks
  [{{user-id :user/id} :request} {:tab/keys [id password] :as input}]
  {::pc/params #{:tab/id}
   ::pc/output [:tab/id {:tab/bookmarks bookmark-output}]}
  ;; Fetch tab and verify against the provided password.
  (let [tab-input          (merge input {:tab/user-id user-id})
        bookmark-input     #:bookmark{:tab-id id :user-id user-id}
        fetch-bookmarks-fn #(do
                              (log/info "Fetching all bookmarks within tab" id "for user" user-id)
                              (let [bookmarks {:tab/id id :tab/bookmarks (db/fetch-bookmarks bookmark-input)}]
                                (log/info "User" user-id "fetched bookmarks within tab" id "successfully")
                                bookmarks))]
    (if-let [err (or (m/explain s/tab-fetch-spec tab-input) (m/explain s/bookmark-multi-fetch-spec bookmark-input))]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
      (if-let [pwd (-> tab-input tab-db/fetch-tab :tab/password)]
        (if (hashers/check password pwd)
          ;; Fetch all bookmarks related to this tab.
          (fetch-bookmarks-fn)
          (do
            (log/warn "User" user-id "attempted to fetch tab" id "with wrong password")
            (throw (ex-info "Invalid input" {:error-type :wrong-password}))))
        (fetch-bookmarks-fn)))))

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
