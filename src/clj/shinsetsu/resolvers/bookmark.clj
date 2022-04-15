(ns shinsetsu.resolvers.bookmark
  (:require
    [buddy.hashers :as hashers]
    [com.wsscode.pathom.connect :as pc]
    [malli.core :as m]
    [malli.error :as me]
    [shinsetsu.db.bookmark :as db]
    [shinsetsu.db.tab :as tab-db]
    [shinsetsu.schema :as s]
    [taoensso.timbre :as log]))

(def bookmark-output [:bookmark/id :bookmark/title :bookmark/url :bookmark/image :bookmark/created :bookmark/updated])
(defn trim-bookmark [b] (dissoc b :bookmark/user-id :bookmark/tab-id))

(pc/defresolver bookmark-resolver
  [{{user-id :user/id} :request} {bookmark-id :bookmark/id :as input}]
  {::pc/input  #{:bookmark/id}
   ::pc/output bookmark-output}
  (let [input (merge {:bookmark/user-id user-id} input)]
    (if-let [err (m/explain s/bookmark-fetch-spec input)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
      (do
        (log/info "Fetching bookmark" bookmark-id "for user" user-id)
        (let [bookmark (-> #:bookmark{:id bookmark-id :user-id user-id} db/fetch-bookmark trim-bookmark)]
          (log/info "User" user-id "fetched bookmark successfully")
          bookmark)))))

(pc/defresolver bookmarks-resolver
  [{{user-id :user/id} :request :as env} {:tab/keys [id] :as input}]
  {::pc/input  #{:tab/id}
   ::pc/output [{:tab/bookmarks bookmark-output}]
   ::pc/params [:tab/password]}
  (log/spy (-> env :query-params))
  ;; Fetch tab and verify against the provided password.
  (let [password       (log/spy (-> env :query-params :tab/password))
        tab-input      (if password
                         (merge input {:tab/password password :tab/user-id user-id})
                         (merge input {:tab/user-id user-id}))
        bookmark-input #:bookmark{:tab-id id :user-id user-id}
        callback       (fn []
                         (log/info "Fetching all bookmarks within tab" id "for user" user-id)
                         (let [bookmarks {:tab/id        id
                                          :tab/bookmarks (->> bookmark-input db/fetch-bookmarks (mapv trim-bookmark))}]
                           (log/info "User" user-id "fetched bookmarks within tab" id "successfully")
                           bookmarks))]
    (if-let [err (or (m/explain s/tab-fetch-spec tab-input) (m/explain s/bookmark-multi-fetch-spec bookmark-input))]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
      (if-let [pwd (-> tab-input tab-db/fetch-tab :tab/password)]
        ;; FIXME: Need to do this to continue with development.
        (if (hashers/check password pwd)
          ;; Fetch all bookmarks related to this tab.
          (callback)
          (do
            (log/warn "User" user-id "attempted to fetch tab" id "with wrong password")
            (throw (ex-info "Invalid input" {:error-type :wrong-password}))))
        (callback)))))

(comment
  (let [a 1]
    (list [test a]))
  (user/restart))
