(ns shinsetsu.db.tag
  (:require [schema.core :as s]
            [shinsetsu.schemas :refer :all]
            [next.jdbc :as nj]
            [honey.sql.helpers :as helpers]
            [honey.sql :as sql]
            [shinsetsu.db.core :refer [db]]))

(s/defn read-tag :- Tag
  [{:tag/keys [id]} :- {:tag/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/select :*)
                            (helpers/from :tag)
                            (helpers/where [:= :tag/id id])
                            (sql/format {:dialect :ansi})))))

(s/defn create-tag :- Tag
  [data :- Tag]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/insert-into :tag)
                            (helpers/values [data])
                            (sql/format {:dialect :ansi})))))

(s/defn update-tag :- PartialTag
  [{:tag/keys [id] :as data} :- PartialTag]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/update :tag)
                            (helpers/set data)
                            (helpers/where [:= :tag/id ~id])
                            (sql/format {:dialect :ansi})))))

(s/defn delete-tag :- Tag
  [{:tag/keys [id]} :- {:tag/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/delete-from :tag)
                            (helpers/where [:= :tag/id id])
                            (sql/format {:dialect :ansi})))))
