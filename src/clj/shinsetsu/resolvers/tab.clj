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
                    (fn [acc {:bookmark/keys [tab-id] :tab/keys [name] :as it}]
                      (let [bookmark (dissoc it :bookmark/tab-id :bookmark/user-id)]
                        (-> acc
                            (assoc-in [tab-id :tab/name] name)
                            (assoc-in [tab-id :tab/is-protected?] false)
                            (update-in [tab-id :tab/bookmarks] conj bookmark))))
                    {}
                    bookmarks)]
    (mapv (fn [[k v]] (merge {:tab/id k} (update v :tab/bookmarks vec))) bookmarks)))

(pc/defresolver tabs-resolver
  [{{user-id :user/id} :request :as env} _]
  {::pc/output [{:user/tabs tab-output}]}
  (let [input {:tab/user-id user-id}]
    (if-let [err (m/explain s/tab-multi-fetch-spec input)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
      (do
        (log/info "Fetching all tabs for user" user-id)
        (let [tabs {:user/tabs (if-let [query (-> env :query-params :tab/query)]
                                 (process-queried-tabs query user-id)
                                 (map trim-tab (tab-db/fetch-tabs {:tab/user-id user-id})))}]
          (log/info "User" user-id "fetched tabs successfully")
          tabs)))))

(comment
  (require '[user])
  (user/restart)
  (some #(println %) {:a 1 :b 2})
  (coll? {:a 1})
  (letfn [(dfs [it]
            (cond
              (map? it) (or (:error it) (some (fn [v] (dfs (second v))) it))
              (coll? it) (some #(dfs %) it)
              :else false))]
    (dfs {:com.wsscode.pathom.core/errors                        {[[:tab/id
                                                                    #uuid "bc1599de-e58a-4d3b-b61b-b0e50b6a2749"]
                                                                   :tab/bookmarks] {}},
          [:tab/id #uuid "bc1599de-e58a-4d3b-b61b-b0e50b6a2749"] {:tab/bookmarks :com.wsscode.pathom.core/reader-error
                                                                  :tab/foo       ["hello" "there"]}}))

  (loop [i [1 2 3]] (if (odd? (first i)) true (recur (rest i))))
  (let [query {:bool {:must [{:bool {:should [{:match {:name {:query "fe", :operator "or"}}}
                                              {:match_phrase {:name "net"}}]}}
                             {:bool {:should [{:match {:tag {:query "ent", :operator "or"}}}
                                              {:match_phrase {:tag "new"}}]}}]}}]
    (process-queried-tabs
      query
      (java.util.UUID/fromString "983650c1-5137-4595-8e83-f2aa3a6fc545"))))
