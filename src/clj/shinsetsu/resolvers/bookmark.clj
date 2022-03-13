(ns shinsetsu.resolvers.bookmark
  (:require
    [com.wsscode.pathom.connect :as pc :refer [defresolver]]
    [shinsetsu.db.bookmark :as db]
    [taoensso.timbre :as log])
  (:import [java.util UUID]))

(defresolver bookmark-resolver
  [{{user-id :user/id} :request} {tab-id :tab/id}]
  {::pc/input  #{:tab/id}
   ::pc/output [:bookmark/id :bookmark/title :bookmark/url :bookmark/image :bookmark/created :bookmark/updated]}
  (log/info "Fetching all bookmarks from user" user-id)
  (log/spy (db/fetch-bookmarks {:tab/id  (if (uuid? tab-id) tab-id (UUID/fromString tab-id))
                                :user/id user-id})))
