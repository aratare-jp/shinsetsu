(ns harpocrates.db.bookmark
  (:require [schema.core :as s]
            [harpocrates.spec :as hs]
            [harpocrates.db.core :refer [db crud-fns]]))

(declare
  Bookmark
  Bookmark?
  create-bookmark
  read-bookmark
  update-bookmark
  delete-bookmark)

(def Bookmark
  {:bookmark/id      s/Uuid
   :bookmark/title   s/Str
   :bookmark/url     s/Str
   :bookmark/image   s/Any
   :bookmark/created s/Inst
   :bookmark/updated s/Inst
   :bookmark/user-id s/Uuid
   :bookmark/tab-id  s/Uuid})

(crud-fns bookmark Bookmark)
