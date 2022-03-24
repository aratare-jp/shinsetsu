(ns shinsetsu.resolvers.tab
  (:require
    [com.wsscode.pathom.connect :as pc :refer [defresolver]]
    [shinsetsu.db.tab :as tab-db]
    [taoensso.timbre :as log]
    [buddy.hashers :as hashers]))

(def tab-output [:tab/id :tab/name :tab/is-protected?])

(defn trim-tab
  [t]
  (-> t
      (assoc :tab/is-protected? (boolean (:tab/password t)))
      (select-keys tab-output)))

(defresolver tabs-resolver
  "Fetch all the tabs that belong to a user."
  [{{user-id :user/id} :request} _]
  {::pc/output [{:user/tabs tab-output}]}
  (log/info "User" user-id "requested all tabs")
  {:user/tabs (map trim-tab (tab-db/fetch-tabs {:tab/user-id user-id}))})
