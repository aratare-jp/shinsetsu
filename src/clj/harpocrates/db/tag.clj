(ns harpocrates.db.tag
  (:require [schema.core :as s]
            [harpocrates.spec :as hs]
            [harpocrates.db.core :refer [db crud-fns]]
            [next.jdbc :as nj]
            [honeysql.helpers :as helpers]
            [honeysql.core :as sql]))

(def Tag
  {:tag/id      s/Uuid
   :tag/name    s/Str
   :tag/colour  (hs/MaxLengthStr 10)
   :tag/image   s/Any
   :tag/created s/Inst
   :tag/updated s/Inst
   :tag/tag-id  s/Uuid})

(def Tag? (into {} (map (fn [[k v]] [(s/optional-key ~k) (s/maybe v)]) Tag)))

(s/defn read-tag :- Tag
  [{:tag/keys [id]} :- {:tag/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/select :*)
                            (helpers/from :tag)
                            (helpers/where [:= :tag/id id])
                            (sql/format :quoting :ansi)))))

(s/defn create-tag :- Tag
  [data :- Tag]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/insert-into :tag)
                            (helpers/values [data])
                            (sql/format :quoting :ansi)))))

(s/defn update-tag :- Tag?
  [{:tag/keys [id] :as data} :- Tag?]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/update :tag)
                            (helpers/sset data)
                            (helpers/where [:= :tag/id ~id])
                            (sql/format :quoting :ansi)))))

(s/defn delete-tag :- Tag
  [{:tag/keys [id]} :- {:tag/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/delete-from :tag)
                            (helpers/where [:= :tag/id id])
                            (sql/format :quoting :ansi)))))
