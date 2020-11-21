(ns harpocrates.resolvers.bookmark
  (:require
    [com.wsscode.pathom.connect :refer [defresolver]]
    [harpocrates.utility :refer [qualify-km]]
    [harpocrates.db.bookmark :refer [Bookmark]]))

(defresolver bookmark-resolver [_ {:bookmark/keys [id]}]
  {::pc/input  #{:bookmark/id}
   ::pc/output [:bookmark/id
                :bookmark/url
                :bookmark/created-at
                :bookmark/updated-at
                {:bookmark/user_id [:user/id]}]}
  (Bookmark :id id))
