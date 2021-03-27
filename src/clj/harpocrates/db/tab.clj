(ns harpocrates.db.tab
  (:require [schema.core :as s]
            [harpocrates.spec :as hs]
            [harpocrates.db.core :refer [db crud-fns]]))

(declare
  Tab
  Tab?
  create-tab
  read-tab
  update-tab
  delete-tab)

(def Tab
  {:tab/id       s/Uuid
   :tab/name     s/Str
   :tab/password s/Str
   :tab/created  s/Inst
   :tab/updated  s/Inst
   :tab/user-id  s/Uuid})

(crud-fns tab Tab)
