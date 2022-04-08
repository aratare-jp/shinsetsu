(ns shinsetsu.resolvers.bookmark-tag
  (:require
    [com.wsscode.pathom.connect :as pc :refer [defresolver]]
    [taoensso.timbre :as log]
    [shinsetsu.db.bookmark-tag :as bookmark-tag-db]
    [malli.core :as m]
    [shinsetsu.schema :as s]
    [malli.error :as me]))

(defresolver bookmark-tag-resolver-by-bookmark
  [{{user-id :user/id} :request} {:bookmark/keys [id]}]
  {::pc/input  #{:bookmark/id}
   ::pc/output [{:bookmark/tags [:tag/id]}]}
  (let [input #:bookmark{:id id :user-id user-id}]
    (if-let [err (m/explain s/bookmark-tag-fetch-by-bookmark-spec input)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)})))
    (log/info "Fetching tags for bookmark" id "for user" user-id)
    (let [tags (bookmark-tag-db/fetch-tags-by-bookmark input)]
      (log/info "All tags assigned to bookmark" id "fetched successfully for user" user-id)
      {:bookmark/tags (mapv (fn [e] {:tag/id (:bookmark-tag/tag-id e)}) tags)})))
