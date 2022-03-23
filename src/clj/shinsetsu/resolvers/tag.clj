(ns shinsetsu.resolvers.tag
  (:require
    [com.wsscode.pathom.connect :as pc :refer [defresolver]]
    [shinsetsu.db.bookmark :as bookmark-db]
    [shinsetsu.db.tag :as tag-db]
    [taoensso.timbre :as log]))

(def tag-output [:tag/id :tag/name :tag/colour :tag/created :tag/updated])

(defresolver tags-resolver
  "Fetch all the tags that belong to a user."
  [{{user-id :user/id} :request} _]
  {::pc/output [{:user/tags tag-output}]}
  (log/info "User" user-id "requested all tags")
  {:user/tags (tag-db/fetch-tags {:user/id user-id})})

(defresolver tag-resolver
  "Fetch a specific tag that belongs to a user"
  [{{user-id :user/id} :request} {tag-id :tag/id}]
  {::pc/input  #{:tag/id}
   ::pc/output tag-output}
  (log/info "User" user-id "requested tag" tag-id)
  (if-let [tag (tag-db/fetch-tag {:user/id user-id :tag/id tag-id})]
    (dissoc tag :tag/password)))

(defresolver bookmark-tag-resolver
  [{{user-id :user/id} :request} {bookmark-id :bookmark/id}]
  {::pc/input  #{:bookmark/id}
   ::pc/output [{:bookmark/tags tag-output}]}
  (log/info "Fetching tags for bookmark" bookmark-id "for user" user-id)
  (let [bookmarks (bookmark-db/fetch-tags-by-bookmark {:bookmark/id bookmark-id :user/id user-id})]
    {:bookmark/tags (mapv (fn [e] {:tag/id (:bookmark-tag/tag-id e)}) bookmarks)}))
