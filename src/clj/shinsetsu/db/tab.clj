(ns shinsetsu.db.tab
  (:require [schema.core :as s]
            [shinsetsu.schemas :refer :all]
            [next.jdbc :as nj]
            [honey.sql.helpers :as helpers]
            [honey.sql :as sql]
            [shinsetsu.db.core :refer [db]]))

(s/defn read-tab :- Tab
  [{:tab/keys [id]} :- {:tab/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/select :*)
                            (helpers/from :tab)
                            (helpers/where [:= :tab/id id])
                            (sql/format {:dialect :ansi})))))

(s/defn create-tab :- Tab
  [data :- Tab]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/insert-into :tab)
                            (helpers/values [data])
                            (sql/format {:dialect :ansi}))))
  data)

(s/defn update-tab :- Tab?
  [{:tab/keys [id] :as data} :- Tab?]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/update :tab)
                            (helpers/set data)
                            (helpers/where [:= :tab/id ~id])
                            (sql/format {:dialect :ansi}))))
  data)

(s/defn delete-tab :- Tab
  [{:tab/keys [id]} :- {:tab/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/delete-from :tab)
                            (helpers/where [:= :tab/id id])
                            (sql/format {:dialect :ansi})))))

(s/defn read-tab-bookmark :- [Bookmark]
  [{:tab/keys [id]} :- {:tab/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/select :bookmark)
                            (helpers/where [:= :bookmark/tab-id id])
                            (sql/format {:dialect :ansi})))))

(s/defn read-tab-tag :- [Tag]
  [{:tab/keys [id]} :- {:tab/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute! tx (-> (helpers/select :tab-tag)
                        (helpers/where [:= :tab-id id])
                        (sql/format {:dialect :ansi})))))

(s/defn create-tab-tag
  [data :- {:tab/id s/Uuid :bookmark/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/insert-into :tab-tag)
                            (helpers/values [data])
                            (sql/format {:dialect :ansi})))))

(s/defn delete-tab-tag :- {:tab-id s/Uuid :tag-id s/Str}
  [{:tab/keys [tag-id] :bookmark/keys [bookmark-id]} :- {:tab/id s/Uuid :bookmark/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/delete-from :tab-tag)
                            (helpers/where [:= :tag-id tag-id] [:= :bookmark-id bookmark-id])
                            (sql/format {:dialect :ansi})))))