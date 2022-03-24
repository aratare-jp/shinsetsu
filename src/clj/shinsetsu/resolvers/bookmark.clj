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

(defresolver bookmarks-resolver
  [{{user-id :user/id} :request} {tab-id :tab/id}]
  {::pc/input  #{:tab/id}
   ::pc/output [{:tab/bookmarks bookmark-output}]}
  (try
    (let [input {:bookmark/tab-id tab-id :bookmark/user-id user-id}]
      (if-let [err (m/explain s/bookmark-multi-fetch-spec input)]
        (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
        (do
          (log/info "Fetching all bookmarks within tab" tab-id "for user" user-id)
          (let [bookmarks {:tab/bookmarks (db/fetch-bookmarks #:bookmark{:tab-id tab-id :user-id user-id})}]
            (log/info "User" user-id "fetched bookmarks within tab" tab-id "successfully")
            bookmarks))))
    (catch ExceptionInfo e
      (let [message (ex-message e)
            data    (as-> e $
                          (ex-data $)
                          (if-let [tab-id-data (get-in $ [:error-data :bookmark/tab-id])]
                            (-> $
                                (assoc-in [:error-data :tab/id] tab-id-data)
                                (dissoc-in [:error-data :bookmark/tab-id]))
                            $))]
        (throw (ex-info message data))))))
