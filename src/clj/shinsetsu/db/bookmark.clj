(ns shinsetsu.db.bookmark
  (:require [schema.core :as s]
            [shinsetsu.schemas :refer :all]
            [next.jdbc :as nj]
            [honey.sql.helpers :as helpers]
            [honey.sql :as sql]
            [shinsetsu.db.core :refer [db]]))

(s/defn read-bookmark :- Bookmark
  [{:bookmark/keys [id]} :- {:bookmark/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/select :*)
                            (helpers/from :bookmark)
                            (helpers/where [:= :bookmark/id id])
                            (sql/format {:dialect :ansi})))))

(s/defn create-bookmark :- Bookmark
  [data :- Bookmark]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/insert-into :bookmark)
                            (helpers/values [data])
                            (sql/format {:dialect :ansi}))))
  data)

(s/defn update-bookmark :- Bookmark
  [{:bookmark/keys [id] :as data} :- PartialBookmark]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/update :bookmark)
                            (helpers/set data)
                            (helpers/where [:= :bookmark/id ~id])
                            (sql/format {:dialect :ansi}))))
  data)

(s/defn delete-bookmark :- Bookmark
  [{:bookmark/keys [id]} :- {:bookmark/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/delete-from :bookmark)
                            (helpers/where [:= :bookmark/id id])
                            (sql/format {:dialect :ansi})))))

(s/defn read-bookmark-tag :- [Tag]
  [{:bookmark/keys [id]} :- {:bookmark/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute! tx (-> (helpers/select :bookmark-tag)
                        (helpers/where [:= :bookmark-id id])
                        (sql/format {:dialect :ansi})))))

(s/defn create-bookmark-tag :- {:bookmark/id s/Uuid :tag-id s/Uuid}
  [data :- {:bookmark/id s/Uuid :tag-id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/insert-into :tab-tag)
                            (helpers/values [data])
                            (sql/format {:dialect :ansi})))))

(s/defn delete-bookmark-tag :- {:bookmark/id s/Uuid :tag-id s/Uuid}
  [{:bookmark/keys [bookmark-id] :tag/keys [tag-id]} :- {:bookmark/id s/Uuid :tag-id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/delete-from :tab-tag)
                            (helpers/where [:= :tag-id tag-id] [:= :bookmark-id bookmark-id])
                            (sql/format {:dialect :ansi})))))