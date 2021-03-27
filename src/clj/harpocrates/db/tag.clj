(ns harpocrates.db.tag
  (:require [schema.core :as s]
            [harpocrates.spec :as hs]
            [harpocrates.db.core :refer [db crud-fns]]))

(declare
  Tag
  Tag?
  create-tag
  read-tag
  update-tag
  delete-tag)

(def Tag
  {:tag/id      s/Uuid
   :tag/name    s/Str
   :tag/colour  (hs/MaxLengthStr 10)
   :tag/image   s/Any
   :tag/created s/Inst
   :tag/updated s/Inst
   :tag/user-id s/Uuid})

(crud-fns tag Tag)
