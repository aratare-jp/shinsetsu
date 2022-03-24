(ns shinsetsu.resolvers.tag
  (:require
    [com.wsscode.pathom.connect :as pc :refer [defresolver]]
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
  (tag-db/fetch-tag {:user/id user-id :tag/id tag-id}))
