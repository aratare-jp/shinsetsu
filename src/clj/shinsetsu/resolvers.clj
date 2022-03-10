(ns shinsetsu.resolvers
  (:require
    [com.wsscode.pathom.core :as p]
    [com.wsscode.pathom.connect :as pc :refer [defresolver]]
    [shinsetsu.db :as db]
    [taoensso.timbre :as log]))

(defresolver tab-resolver
  [{{user-id :user/id} :request :as env} _]
  {::pc/output [{:tab/ids [:tab/id :tab/name :tab/created :tab/updated]}]}
  (log/info "Fetching all tabs")
  {:tab/ids (db/fetch-tabs user-id)})

(def public-resolvers [])
(def protected-resolvers [tab-resolver])

(comment
  (require '[shinsetsu.db :as db])
  (db/fetch-tabs (java.util.UUID/fromString "ac5cc571-3029-4409-b2f5-e7c68c4b9a5f"))
  (require '[taoensso.timbre :as log])
  (log/info "test")
  (user/restart))
