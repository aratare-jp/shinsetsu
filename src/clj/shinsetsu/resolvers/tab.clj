(ns shinsetsu.resolvers.tab
  (:require
    [com.wsscode.pathom.connect :as pc]
    [malli.core :as m]
    [malli.error :as me]
    [shinsetsu.db.bookmark :as bookmark-db]
    [shinsetsu.db.tab :as tab-db]
    [shinsetsu.schema :as s]
    [taoensso.timbre :as log]))

(def tab-output [:tab/id :tab/name :tab/is-protected?])

(defn trim-tab
  [t]
  (-> t
      (assoc :tab/is-protected? (boolean (:tab/password t)))
      (select-keys tab-output)))

(defn- process-queried-tabs
  [query user-id]
  (let [bookmarks (->> (bookmark-db/fetch-bookmarks-with-query {:bookmark/user-id user-id} query)
                       (mapv #(dissoc % :bookmark/image)))
        bookmarks (reduce
                    (fn [acc {:bookmark/keys [tab-id] :as it}]
                      (let [bookmark (dissoc it :bookmark/tab-id :bookmark/user-id)]
                        (update-in acc [tab-id :tab/bookmarks] conj bookmark)))
                    {}
                    bookmarks)]
    (mapv (fn [[k v]] {:tab/id k :tab/bookmarks (vec (:tab/bookmarks v))}) bookmarks)))

(pc/defresolver tabs-resolver
  [{{user-id :user/id} :request :as env} _]
  {::pc/output [{:user/tabs tab-output}]}
  (let [{:keys [query]} (:query-params env)
        input {:tab/user-id user-id}]
    (if-let [err (m/explain s/tab-multi-fetch-spec input)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
      (do
        (log/info "Fetching all tabs for user" user-id)
        (let [tabs {:user/tabs (if query
                                 (process-queried-tabs query user-id)
                                 (map trim-tab (tab-db/fetch-tabs {:tab/user-id user-id})))}]
          (log/info "User" user-id "fetched tabs successfully")
          tabs)))))

(comment
  (require '[user])
  (user/restart)
  (mapv (fn [[k v]] v) {:a 1 :b 2})
  (let [query {:bool {:must [{:bool {:should [{:match {:name {:query "fe", :operator "or"}}}
                                              {:match_phrase {:name "net"}}]}}
                             {:bool {:should [{:match {:tag {:query "ent", :operator "or"}}}
                                              {:match_phrase {:tag "new"}}]}}]}}]
    (process-queried-tabs
      query
      (java.util.UUID/fromString "983650c1-5137-4595-8e83-f2aa3a6fc545"))))
