(ns shinsetsu.resolvers.tag
  (:require
    [com.wsscode.pathom.connect :as pc :refer [defresolver]]
    [shinsetsu.db.tag :as tag-db]
    [taoensso.timbre :as log]
    [malli.error :as me]
    [shinsetsu.schema :as s]
    [malli.core :as m]))

(def tag-output [:tag/id :tag/name :tag/colour :tag/created :tag/updated])

(defresolver tag-resolver
  "Fetch a specific tag that belongs to a user"
  [{{user-id :user/id} :request} {:tag/keys [id] :as input}]
  {::pc/input  #{:tag/id}
   ::pc/output tag-output}
  (let [input (merge input {:tag/user-id user-id})]
    (if-let [err (m/explain s/tag-fetch-spec input)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)})))
    (log/info "User" user-id "requested tag" id)
    (let [tag (tag-db/fetch-tag input)]
      (log/info "Tag" id "fetched from user" user-id)
      tag)))
