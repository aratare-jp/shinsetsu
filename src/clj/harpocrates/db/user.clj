(ns harpocrates.db.user
  (:require [toucan.models :as tc]
            [harpocrates.db.shared :refer :all]))

(tc/defmodel User :users
  tc/IModel
  (tc/properties
    [_]
    {:uuid?        true
     :timestamped? true})
  (tc/types
    [_]
    {:id       :uuid
     :password :tc-password}))
