(ns shinsetsu.mutations.bookmark-tag
  (:require
    [shinsetsu.db.bookmark-tag :as db]
    [com.wsscode.pathom.connect :as pc :refer [defmutation]]
    [malli.core :as m]
    [shinsetsu.schema :as s]
    [malli.error :as me]
    [taoensso.timbre :as log]))

(defmutation create-bookmark-tag
  [{{user-id :user/id} :request} {:bookmark-tag/keys [bookmark-id tag-id] :as input}]
  {::pc/params #{:bookmark-tag/bookmark-id :bookmark-tag/tag-id}
   ::pc/output [:bookmark/id :tag/id]}
  (let [input (assoc input :bookmark-tag/user-id user-id)]
    (if-let [err (m/explain s/bookmark-tag-create-spec input)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
      (do
        (log/info "User with id" user-id "is attempting to assign tag" tag-id "to bookmark" bookmark-id)
        (db/create-bookmark-tag input)
        (log/info "User with ID" user-id "assigned tag" tag-id "to bookmark" bookmark-id "successfully")
        {:bookmark/id bookmark-id :tag/id tag-id}))))

(defmutation delete-bookmark-tag
  [{{user-id :user/id} :request} {:bookmark-tag/keys [bookmark-id tag-id] :as input}]
  {::pc/params #{:bookmark/id :tag/id}
   ::pc/output [:bookmark/id :tag/id]}
  (let [input (assoc input :bookmark-tag/user-id user-id)]
    (if-let [err (m/explain s/bookmark-tag-delete-spec input)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
      (do
        (log/info "User with id" user-id "is attempting to delete assignment of tag" tag-id "to bookmark" bookmark-id)
        (db/delete-bookmark-tag input)
        (log/info "User with ID" user-id "deleted assignment of tag" tag-id "to bookmark" bookmark-id "successfully")
        {:bookmark/id bookmark-id :tag/id tag-id}))))
