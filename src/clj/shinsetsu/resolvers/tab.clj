(ns shinsetsu.resolvers.tab
  (:require [com.wsscode.pathom.connect :as pc]
            [shinsetsu.db.core :refer [db]]
            [shinsetsu.schemas :refer :all]
            [taoensso.timbre :as log]
            [buddy.sign.jws :as jws]
            [shinsetsu.config :refer [env]]
            [puget.printer :refer [pprint]]))

(pc/defresolver tab-resolver
  [env {:tab/keys [id]}]
  {::pc/input  #{:tab/id}
   ::pc/output [:tab/name {:tab/bookmarks [:bookmark/id]}]}
  (pprint (get-in @db [:tab/id id]))
  (let [{:tab/keys [name bookmarks]} (get-in @db [:tab/id id])]
    {:tab/id        id
     :tab/name      name
     :tab/bookmarks bookmarks}))

(def resolvers [tab-resolver])