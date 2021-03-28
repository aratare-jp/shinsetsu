(ns shinsetsu.resolvers.bookmark
  (:require [com.wsscode.pathom.connect :as pc]
            [shinsetsu.db.core :refer [db]]
            [shinsetsu.schemas :refer :all]
            [taoensso.timbre :as log]
            [buddy.sign.jws :as jws]
            [shinsetsu.config :refer [env]]
            [puget.printer :refer [pprint]]))

(pc/defresolver bookmark-resolver
  [env {:bookmark/keys [id]}]
  {::pc/input  #{:bookmark/id}
   ::pc/output [:bookmark/url :bookmark/name]}
  (let [{:bookmark/keys [name url]} (get-in @db [:bookmark/id id])]
    {:bookmark/id   id
     :bookmark/name name
     :bookmark/url  url}))

(def resolvers [bookmark-resolver])