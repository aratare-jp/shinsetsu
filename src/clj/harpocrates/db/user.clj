(ns harpocrates.db.user
  (:require [schema.core :as s]
            [harpocrates.spec :as hs]
            [harpocrates.db.core :refer [db crud-fns]]))

(declare
  User
  User?
  create-user
  read-user
  update-user
  delete-user)

(def User
  {:user/id       s/Uuid
   :user/username hs/NonEmptyContinuousStr
   :user/password hs/NonEmptyContinuousStr
   :user/created  s/Inst
   :user/updated  s/Inst})

(crud-fns user User)
