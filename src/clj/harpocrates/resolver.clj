(ns harpocrates.resolver
  (:require
    [com.wsscode.pathom.core :as p]
    [com.wsscode.pathom.connect :as pc]
    [harpocrates.db.core :as db]
    [harpocrates.utility :refer [qualify-km]])
  (:import (java.util UUID)))

(pc/defresolver bookmark-resolver [_ {:bookmark/keys [id]}]
  {::pc/input  #{:bookmark/id}
   ::pc/output [:bookmark/url :bookmark/timestamp]}
  (-> (db/get-bookmark! db/*db* {:bookmark/id (UUID/fromString id)})
      (qualify-km "bookmark")))

(pc/defresolver user-resolver [_ {:user/keys [id]}]
  {::pc/input  #{:user/id}
   ::pc/output [:user/first-name
                :user/last-name
                :user/email
                :user/password
                :user/is-active
                :user/last-login
                {:user/bookmarks [:bookmark/id]}]}
  (let [query    {:user/id (UUID/fromString id)}
        user     (db/get-user! db/*db* query)
        bookmark (db/get-bookmark-from-user! db/*db* query)]
    (-> (assoc user :bookmarks bookmark)
        (qualify-km "user"))))

(def resolvers [bookmark-resolver
                user-resolver])
