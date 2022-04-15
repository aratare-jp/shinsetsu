(ns shinsetsu.mutations.bookmark
  (:require
    [shinsetsu.db.bookmark :as db]
    [com.wsscode.pathom.connect :as pc :refer [defmutation]]
    [taoensso.timbre :as log]
    [malli.core :as m]
    [malli.error :as me]
    [shinsetsu.schema :as s]
    [shinsetsu.db.bookmark-tag :as btdb]
    [com.fulcrologic.fulcro.networking.file-upload :as fu])
  (:import [java.nio.file Files]
           [java.util Base64]))

(def bookmark-input #{:bookmark/title :bookmark/url :bookmark/image :bookmark/user-id :bookmark/tab-id :bookmark/add-tags :bookmark/remove-tags ::fu/files})
(def bookmark-output [:bookmark/id :bookmark/title :bookmark/url :bookmark/image :bookmark/favourite :bookmark/created :bookmark/updated])

(defn trim-bookmark
  [b]
  (dissoc b :bookmark/user-id :bookmark/tab-id))

(defn image->base64
  [{:keys [type] :as f}]
  (some->> f
           .toPath
           Files/readAllBytes
           (.encodeToString (Base64/getEncoder))
           (str "data:" type ";base64,")))

(pc/defmutation create-bookmark
  [{{user-id :user/id} :request} {:bookmark/keys [add-tags] :as bookmark}]
  {::pc/params bookmark-input
   ::pc/output bookmark-output}
  (let [bookmark (-> bookmark
                     (assoc :bookmark/user-id user-id)
                     (dissoc :bookmark/add-tags))]
    (if-let [err (or (m/explain s/bookmark-create-spec bookmark) (m/explain [:maybe [:vector :uuid]] add-tags))]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)})))
    (log/info "User with id" user-id "is attempting to create a new bookmark")
    (let [image    (some->> bookmark ::fu/files first :tempfile image->base64)
          tempid   (:bookmark/id bookmark)
          bookmark (-> bookmark
                       (dissoc :bookmark/id ::fu/files :bookmark/add-tags :bookmark/remove-tags)
                       ((fn [b] (if image (assoc b :bookmark/image image) b)))
                       db/create-bookmark
                       trim-bookmark)
          real-id  (:bookmark/id bookmark)]
      (if (not (empty? add-tags))
        (do
          (log/info "Assigning tags" add-tags "to bookmark" real-id)
          (btdb/create-bookmark-tags #:bookmark-tag{:bookmark-id real-id :tag-ids add-tags :user-id user-id})
          (log/info "Tags" add-tags "assigned to bookmark" real-id "successfully")))
      (log/info "User with ID" user-id "created bookmark" real-id "successfully")
      (if tempid
        (merge bookmark {:tempids {tempid real-id}})
        bookmark))))

(pc/defmutation patch-bookmark
  [{{user-id :user/id} :request} {:bookmark/keys [id add-tags remove-tags] :as bookmark}]
  {::pc/params bookmark-input
   ::pc/output bookmark-output}
  (let [bookmark (-> bookmark
                     (assoc :bookmark/user-id user-id)
                     (dissoc :bookmark/add-tags :bookmark/remove-tags))]
    (if-let [err (or (m/explain s/bookmark-patch-spec bookmark)
                     (m/explain [:maybe [:vector :uuid]] add-tags)
                     (m/explain [:maybe [:vector :uuid]] remove-tags))]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
      (do
        (log/info "User with id" user-id "is attempting to patch bookmark" id)
        (if (not (empty? add-tags))
          (do
            (log/info "Assigning new tags" add-tags "to bookmark" id)
            (btdb/create-bookmark-tags #:bookmark-tag{:bookmark-id id :tag-ids add-tags :user-id user-id})
            (log/info "New tags" add-tags "assigned to bookmark" id)))
        (if (not (empty? remove-tags))
          (do
            (log/info "Removing new tags" add-tags "from bookmark" id)
            (btdb/delete-bookmark-tags #:bookmark-tag{:bookmark-id id :tag-ids remove-tags :user-id user-id})
            (log/info "Tags" add-tags "removed from bookmark" id)))
        (let [image    (some->> bookmark ::fu/files first :tempfile image->base64)
              bookmark (-> bookmark
                           (dissoc ::fu/files :bookmark/add-tags :bookmark/remove-tags)
                           ((fn [b] (if image (assoc b :bookmark/image image) b)))
                           db/patch-bookmark
                           trim-bookmark)]
          (log/info "User with ID" user-id "patched bookmark" id "successfully")
          bookmark)))))

(pc/defmutation delete-bookmark
  [{{user-id :user/id} :request} {:bookmark/keys [id] :as bookmark}]
  {::pc/params #{:bookmark/id}
   ::pc/output bookmark-output}
  (let [bookmark (assoc bookmark :bookmark/user-id user-id)]
    (if-let [err (m/explain s/bookmark-delete-spec bookmark)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
      (do
        (log/info "User with id" user-id "is attempting to delete bookmark" id)
        (let [bookmark (-> bookmark db/delete-bookmark trim-bookmark)]
          (log/info "User with ID" user-id "deleted bookmark" id "successfully")
          bookmark)))))
