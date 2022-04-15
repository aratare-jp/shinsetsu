(ns shinsetsu.resolvers.tag
  (:require
    [com.wsscode.pathom.connect :as pc :refer [defresolver]]
    [shinsetsu.db.tag :as tag-db]
    [taoensso.timbre :as log]
    [malli.error :as me]
    [shinsetsu.schema :as s]
    [malli.core :as m]))

(def tag-output [:tag/id :tag/name :tag/colour :tag/created :tag/updated])
(defn trim-tag [tag] (-> tag (dissoc :tag/user-id)))

(defresolver tag-resolver
  "Fetch a specific tag that belongs to a user"
  [{{user-id :user/id} :request} {:tag/keys [id] :as input}]
  {::pc/input  #{:tag/id}
   ::pc/output tag-output}
  (let [input (merge input {:tag/user-id user-id})]
    (if-let [err (m/explain s/tag-fetch-spec input)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)})))
    (log/info "User" user-id "requested tag" id)
    (let [tag (-> input tag-db/fetch-tag trim-tag)]
      (log/info "Tag" id "fetched from user" user-id)
      tag)))

(defresolver tags-resolver
  [{{user-id :user/id} :request :as env} _]
  {::pc/output [{:user/tags tag-output}]}
  (let [{:tag/keys [name name-pos]} (-> env :ast :params)
        input #:tag{:name name :name-pos name-pos :user-id user-id}]
    (if-let [err (m/explain s/tag-multi-fetch-spec input)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)})))
    (log/info "User" user-id "requested all tags")
    (let [tags (tag-db/fetch-tags input)]
      (log/info "All tags fetched from user" user-id)
      {:user/tags (mapv trim-tag tags)})))

(comment
  (user/restart))
