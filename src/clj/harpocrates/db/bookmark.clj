(ns harpocrates.db.bookmark
  (:require [toucan.models :as tc]
            [harpocrates.db.shared :as shared]))

(tc/defmodel Bookmark :bookmarks
  tc/IModel
  (tc/properties
    [_]
    {:uuid?        true
     :timestamped? true})
  (tc/types
    [_]
    {:id :uuid}))
