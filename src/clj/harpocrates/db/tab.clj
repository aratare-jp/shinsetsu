(ns harpocrates.db.tab
  (:require [schema.core :as s]
            [harpocrates.spec :as hs]
            [harpocrates.db.core :refer [db crud-fns]]
            [next.jdbc :as nj]
            [honeysql.helpers :as helpers]
            [honeysql.core :as sql]
            [harpocrates.db.bookmark :refer [Bookmark]]
            [harpocrates.db.tag :refer [Tag]]))

(def Tab
  {:tab/id       s/Uuid
   :tab/name     s/Str
   :tab/password s/Str
   :tab/created  s/Inst
   :tab/updated  s/Inst
   :tab/tab-id   s/Uuid})

(def Tab? (into {} (map (fn [[k v]] [(s/optional-key ~k) (s/maybe v)]) Tab)))

(s/defn read-tab :- Tab
  [{:tab/keys [id]} :- {:tab/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/select :*)
                            (helpers/from :tab)
                            (helpers/where [:= :tab/id id])
                            (sql/format :quoting :ansi)))))

(s/defn create-tab :- Tab
  [data :- Tab]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/insert-into :tab)
                            (helpers/values [data])
                            (sql/format :quoting :ansi))))
  data)

(s/defn update-tab :- Tab?
  [{:tab/keys [id] :as data} :- Tab?]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/update :tab)
                            (helpers/sset data)
                            (helpers/where [:= :tab/id ~id])
                            (sql/format :quoting :ansi))))
  data)

(s/defn delete-tab :- Tab
  [{:tab/keys [id]} :- {:tab/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/delete-from :tab)
                            (helpers/where [:= :tab/id id])
                            (sql/format :quoting :ansi)))))

(s/defn read-tab-bookmark :- [Bookmark]
  [{:tab/keys [id]} :- {:tab/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/select :bookmark)
                            (helpers/where [:= :bookmark/tab-id id])
                            (sql/format :quoting :ansi)))))

(s/defn read-tab-tag :- [Tag]
  [{:tab/keys [id]} :- {:tab/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute! tx (-> (helpers/select :tab-tag)
                        (helpers/where [:= :tab-id id])
                        (sql/format :quoting :ansi)))))

(s/defn create-tab-tag
  [data :- {:tab/id s/Uuid :bookmark/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/insert-into :tab-tag)
                            (helpers/values [data])
                            (sql/format :quoting :ansi)))))

(s/defn delete-tab-tag :- {:tab-id s/Uuid :tag-id s/Str}
  [{:tab/keys [tag-id] :bookmark/keys [bookmark-id]} :- {:tab/id s/Uuid :bookmark/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/delete-from :tab-tag)
                            (helpers/where [:= :tag-id tag-id] [:= :bookmark-id bookmark-id])
                            (sql/format :quoting :ansi)))))