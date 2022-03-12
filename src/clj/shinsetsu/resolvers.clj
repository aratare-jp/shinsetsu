(ns shinsetsu.resolvers
  (:require
    [com.wsscode.pathom.core :as p]
    [com.wsscode.pathom.connect :as pc :refer [defresolver]]
    [shinsetsu.db :as db]
    [taoensso.timbre :as log])
  (:import [java.util UUID]))

(defresolver tabs-resolver
  [{{user-id :user/id} :request :as env} _]
  {::pc/output [{:tab/tabs [:tab/id :tab/name :tab/created :tab/updated]}]}
  (log/info "Fetching all tabs from user" user-id)
  {:tab/tabs (db/fetch-tabs {:user/id user-id})})

(defresolver tab-resolver
  [{{user-id :user/id} :request :as env} {tab-id :tab/id}]
  {::pc/input  #{:tab/id}
   ::pc/output [:tab/id :tab/name :tab/created :tab/updated]}
  (log/info "Fetching tab info from user" user-id)
  (db/fetch-tab {:user/id user-id :tab/id tab-id}))

(defresolver tab-bookmarks-resolver
  [{{user-id :user/id} :request :as env} {tab-id :tab/id}]
  {::pc/input  #{:tab/id}
   ::pc/output [:tab/bookmarks]}
  (log/info "Fetching all the bookmarks from tab of user" user-id)
  {:tab/bookmarks (db/fetch-bookmarks {:tab/id  (if (uuid? tab-id) tab-id (UUID/fromString tab-id))
                                       :user/id user-id})})

(defresolver bookmark-resolver
  [{{user-id :user/id} :request :as env} {tab-id :tab/id}]
  {::pc/input  #{:tab/id}
   ::pc/output [:bookmark/id :bookmark/title :bookmark/url :bookmark/image :bookmark/created :bookmark/updated]}
  (log/info "Fetching all bookmarks from user" user-id)
  (log/spy (db/fetch-bookmarks {:tab/id  (if (uuid? tab-id) tab-id (UUID/fromString tab-id))
                                :user/id user-id})))

(def public-resolvers [])
(def protected-resolvers [tabs-resolver tab-resolver tab-bookmarks-resolver bookmark-resolver])

(comment
  (require '[shinsetsu.db :as db])
  (db/fetch-tabs (UUID/fromString "ac5cc571-3029-4409-b2f5-e7c68c4b9a5f"))
  (require '[taoensso.timbre :as log])
  (log/info "test")
  (user/restart))
