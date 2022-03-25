(ns shinsetsu.resolvers.bookmark
  (:require
    [com.wsscode.pathom.connect :as pc :refer [defresolver]]
    [shinsetsu.db.bookmark :as db]
    [taoensso.timbre :as log]
    [malli.core :as m]
    [shinsetsu.schema :as s]
    [malli.error :as me]
    [medley.core :refer [dissoc-in]])
  (:import [clojure.lang ExceptionInfo]))

(def bookmark-output [:bookmark/id :bookmark/title :bookmark/url :bookmark/image :bookmark/created :bookmark/updated])

(defresolver bookmark-resolver
  [{{user-id :user/id} :request} {bookmark-id :bookmark/id :as input}]
  {::pc/input  #{:bookmark/id}
   ::pc/output bookmark-output}
  (let [input (merge {:bookmark/user-id user-id} input)]
    (if-let [err (m/explain s/bookmark-fetch-spec input)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
      (do
        (log/info "Fetching bookmark" bookmark-id "for user" user-id)
        (let [bookmark (db/fetch-bookmark #:bookmark{:id bookmark-id :user-id user-id})]
          (log/info "User" user-id "fetched bookmark successfully")
          bookmark)))))
