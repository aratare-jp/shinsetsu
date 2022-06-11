(ns shinsetsu.resolvers.bookmark
  (:require
    [buddy.hashers :as hashers]
    [com.wsscode.pathom.connect :as pc]
    [malli.core :as m]
    [malli.error :as me]
    [shinsetsu.db.bookmark :as db]
    [shinsetsu.db.tab :as tab-db]
    [shinsetsu.schema :as s]
    [taoensso.timbre :as log])
  (:import [java.sql Timestamp]
           [java.time Duration Instant]))

(def bookmark-output [:bookmark/id :bookmark/title :bookmark/url :bookmark/image :bookmark/created :bookmark/updated])
(defn trim-bookmark [b] (dissoc b :bookmark/user-id :bookmark/tab-id))

;; FIXME: Currently bookmark is not guarded. Need to find a way to guard it with password.
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
   ::pc/output [:tab/lock? {:tab/bookmarks bookmark-output} :tab/bookmark-count]}
  (let [{:keys [password] :as opts} (-> env :query-params)
        tab-input      (merge input {:tab/password password :tab/user-id user-id})
        bookmark-input #:bookmark{:tab-id id :user-id user-id}
        callback       (fn []
                         (log/info "Fetching all bookmarks within tab" id "for user" user-id)
                         (let [bookmarks {:tab/bookmarks (mapv trim-bookmark (db/fetch-bookmarks bookmark-input opts))}]
                           (log/info "User" user-id "fetched bookmarks within tab" id "successfully")
                           bookmarks))]
    (if-let [err (or (m/explain s/tab-fetch-spec tab-input) (m/explain s/bookmark-bulk-fetch-spec bookmark-input))]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)})))
    (if-let [{pwd :tab/password ^Timestamp unlock :tab/unlock} (-> tab-input tab-db/fetch-tab)]
      (cond
        (nil? pwd)
        (callback)
        ;; The user has unlocked this tab some time in the last 15 minutes
        (or
          (.isBefore (Instant/now) (.plus (.toInstant unlock) (Duration/ofMinutes 15)))
          (hashers/check password pwd))
        (do
          (tab-db/patch-tab #:tab{:id id :user-id user-id :unlock (Instant/now)})
          (callback))
        :else
        (do
          (log/warn "User" user-id "attempted to fetch tab" id "with wrong password")
          (throw (ex-info "Invalid input" {:error-type :wrong-password}))))
      (throw (ex-info "Tab does not exist" {:error-type :invalid-tab})))))

(comment
  (nth nil 1)
  (let [a 1]
    (list [test a]))
  (user/restart))
