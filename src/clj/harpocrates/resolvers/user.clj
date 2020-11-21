(ns harpocrates.resolvers.user
  (:require
    [com.wsscode.pathom.connect :refer [defresolver]]
    [harpocrates.utility :refer [qualify-km]]
    [harpocrates.db.user :refer [User]]
    [harpocrates.db.bookmark :refer [Bookmark]]
    [toucan.db :as tc]))

(defresolver user-resolver [_ {:user/keys [id]}]
  {::pc/input  #{:user/id}
   ::pc/output [:user/first-name
                :user/last-name
                :user/email
                :user/password
                :user/is-active
                :user/last-login
                :user/created-at
                :user/updated-at
                {:user/bookmarks [:bookmark/id]}]}
  (merge
    (User :id id)
    (tc/select Bookmark :user_id id)))
