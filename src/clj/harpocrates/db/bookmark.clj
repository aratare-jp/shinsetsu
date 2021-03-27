(ns harpocrates.db.bookmark
  (:require [schema.core :as s]
            [harpocrates.spec :as hs]
            [harpocrates.db.core :refer [db crud-fns]]
            [next.jdbc :as nj]
            [honeysql.helpers :as helpers]
            [honeysql.core :as sql]
            [harpocrates.db.tag :refer [Tag]]))

(def Bookmark
  {:bookmark/id          s/Uuid
   :bookmark/title       s/Str
   :bookmark/url         s/Str
   :bookmark/image       s/Any
   :bookmark/created     s/Inst
   :bookmark/updated     s/Inst
   :bookmark/bookmark-id s/Uuid
   :bookmark/tab-id      s/Uuid})

(def Bookmark? (into {} (map (fn [[k v]] [(s/optional-key ~k) (s/maybe v)]) Bookmark)))

(s/defn read-bookmark :- Bookmark
  [{:bookmark/keys [id]} :- {:bookmark/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/select :*)
                            (helpers/from :bookmark)
                            (helpers/where [:= :bookmark/id id])
                            (sql/format :quoting :ansi)))))

(s/defn create-bookmark :- Bookmark
  [data :- Bookmark]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/insert-into :bookmark)
                            (helpers/values [data])
                            (sql/format :quoting :ansi))))
  data)

(s/defn update-bookmark :- Bookmark
  [{:bookmark/keys [id] :as data} :- Bookmark?]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/update :bookmark)
                            (helpers/sset data)
                            (helpers/where [:= :bookmark/id ~id])
                            (sql/format :quoting :ansi))))
  data)

(s/defn delete-bookmark :- Bookmark
  [{:bookmark/keys [id]} :- {:bookmark/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/delete-from :bookmark)
                            (helpers/where [:= :bookmark/id id])
                            (sql/format :quoting :ansi)))))

(s/defn read-bookmark-tag :- [Tag]
  [{:bookmark/keys [id]} :- {:bookmark/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute! tx (-> (helpers/select :bookmark-tag)
                            (helpers/where [:= :bookmark-id id])
                            (sql/format :quoting :ansi)))))

(s/defn create-bookmark-tag :- {:bookmark/id s/Uuid :tag-id s/Uuid}
  [data :- {:bookmark/id s/Uuid :tag-id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/insert-into :tab-tag)
                            (helpers/values [data])
                            (sql/format :quoting :ansi)))))

(s/defn delete-bookmark-tag :- {:bookmark/id s/Uuid :tag-id s/Uuid}
  [{:bookmark/keys [bookmark-id] :tag/keys [tag-id]} :- {:bookmark/id s/Uuid :tag-id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/delete-from :tab-tag)
                            (helpers/where [:= :tag-id tag-id] [:= :bookmark-id bookmark-id])
                            (sql/format :quoting :ansi)))))