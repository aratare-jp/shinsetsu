(ns shinsetsu.resolvers.bookmark
  (:require
    [com.wsscode.pathom.connect :as pc :refer [defresolver]]
    [shinsetsu.db.bookmark :as db]
    [taoensso.timbre :as log]))

(defresolver bookmark-resolver
  [{{user-id :user/id} :request} {bookmark-id :bookmark/id}]
  {::pc/input  #{:bookmark/id}
   ::pc/output [:bookmark/id :bookmark/title :bookmark/url :bookmark/image :bookmark/created :bookmark/updated]}
  (log/info "Fetching bookmark" bookmark-id "for user" user-id)
  (db/fetch-bookmark {:bookmark/id bookmark-id :user/id user-id}))

(defresolver bookmark-tag-resolver
  [{{user-id :user/id} :request} {bookmark-id :bookmark/id}]
  {::pc/input  #{:bookmark/id}
   ::pc/output [{:bookmark/tags [:tag/name :tag/colour :tag/created :tag/updated]}]}
  (log/info "Fetching tags for bookmark" bookmark-id "for user" user-id)
  {:bookmark/tags (map (fn [e] {:tag/id (:bookmark-tag/tag-id e)})
                       (db/fetch-tags-by-bookmark {:bookmark/id bookmark-id :user/id user-id}))})

(defresolver bookmarks-resolver
  "Fetch all the bookmarks within a tab that belong to a user"
  [{{user-id :user/id} :request} {tab-id :tab/id}]
  {::pc/input  #{:tab/id}
   ::pc/output [{:tab/bookmarks [:bookmark/id :bookmark/title :bookmark/url :bookmark/image :bookmark/created :bookmark/updated]}]}
  (log/info "User" user-id "requested all bookmarks within tab" tab-id)
  {:tab/bookmarks (db/fetch-bookmarks {:tab/id tab-id :user/id user-id})})
