(ns shinsetsu.resolvers.tab
  (:require
    [com.wsscode.pathom.connect :as pc]
    [shinsetsu.db.tab :as tab-db]
    [taoensso.timbre :as log]
    [malli.core :as m]
    [shinsetsu.schema :as s]
    [malli.error :as me]))

(def tab-output [:tab/id :tab/name :tab/is-protected?])

(defn trim-tab
  [t]
  (-> t
      (assoc :tab/is-protected? (boolean (:tab/password t)))
      (select-keys tab-output)))

(pc/defresolver tabs-resolver
  [{{user-id :user/id} :request :as env} _]
  {::pc/params [:tab/query]
   ::pc/output [{:user/tabs tab-output}]}
  (let [query (log/spy (-> env :query-params :tab/query))
        input {:tab/user-id user-id}]
    (if-let [err (m/explain s/tab-multi-fetch-spec input)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
      (do
        (log/info "Fetching all tabs for user" user-id)
        (let [bookmarks {:user/tabs (map trim-tab (tab-db/fetch-tabs {:tab/user-id user-id}))}]
          (log/info "User" user-id "fetched tabs successfully")
          bookmarks)))))

(comment
  (require '[user])
  (user/restart))
