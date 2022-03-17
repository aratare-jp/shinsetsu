(ns shinsetsu.resolvers.tab
  (:require
    [com.wsscode.pathom.connect :as pc :refer [defresolver]]
    [shinsetsu.db.tab :as tab-db]
    [shinsetsu.db.bookmark :as bookmark-db]
    [taoensso.timbre :as log]))

(defn remove-password
  [tab]
  (-> tab
      (assoc :tab/is-protected? ((complement nil?) (:tab/password tab)))
      (dissoc :tab/password)))

(defresolver tabs-resolver
  "Fetch all the tabs that belong to a user."
  [{{user-id :user/id} :request} _]
  {::pc/output [{:tab/tabs [:tab/id :tab/name :tab/created :tab/updated :tab/is-protected?]}]}
  (log/info "User" user-id "requested all tabs")
  {:tab/tabs (map remove-password (tab-db/fetch-tabs {:user/id user-id}))})

(defresolver tab-resolver
  "Fetch a specific tab that belongs to a user"
  [{{user-id :user/id} :request} {tab-id :tab/id}]
  {::pc/input  #{:tab/id}
   ::pc/output [:tab/id :tab/name :tab/created :tab/updated :tab/is-protected?]}
  (log/info "User" user-id "requested tab" tab-id)
  (if-let [tab (tab-db/fetch-tab {:user/id user-id :tab/id tab-id})]
    (remove-password tab)))
