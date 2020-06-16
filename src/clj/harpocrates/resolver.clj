(ns harpocrates.resolver
  (:require
    [com.wsscode.pathom.core :as p]
    [com.wsscode.pathom.connect :as pc]
    [harpocrates.db.core :as db]))

(pc/defresolver bookmark-resolver [env {:bookmark/keys [id]}]
  {::pc/input  #{:bookmark/id}
   ::pc/output [:bookmark/url :bookmark/timestamp]}
  (db/get-bookmark! db/*db* {:bookmark/id id}))

(pc/defresolver user-resolver [env {:user/keys [id]}]
  {::pc/input  #{:user/id}
   ::pc/output [:user/first-name
                :user/last-name
                :user/email
                :user/password
                :user/is-active
                :user/last-login
                {:user/bookmarks [:bookmark/id]}]}
  (let [query    {:user/id id}
        user     (db/get-user! db/*db* query)
        bookmark (db/get-bookmark-from-user! db/*db* query)]
    (assoc user [:user/bookmarks] bookmark)))

(def resolvers [bookmark-resolver
                user-resolver])
