(ns shinsetsu.db.tag
  (:require [schema.core :as s]
            [shinsetsu.schemas :refer :all]
            [next.jdbc :as nj]
            [honey.sql.helpers :as helpers]
            [honey.sql :as sql]
            [puget.printer :refer [pprint]]
            [shinsetsu.db.core :refer [db]]
            [taoensso.timbre :as log]
            [shinsetsu.utility :refer :all]
            [schema-generators.generators :as g]))

(s/defn read-tag :- (s/maybe TagDB)
  [{:tag/keys [id]} :- {:tag/id s/Uuid}]
  (log/info "Reading tag with id" id)
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/select :*)
                            (helpers/from :tag)
                            (helpers/where [:= :tag/id id])
                            (sql/format {:dialect :ansi})))))

(s/defn read-user-tag :- [TagDB]
  [{:user/keys [id]} :- {:user/id s/Uuid}]
  (log/info "Reading tag with user id" id)
  (nj/with-transaction [tx db]
    (nj/execute! tx (-> (helpers/select :*)
                        (helpers/from :tag)
                        (helpers/where [:= :tag/user-id id])
                        (sql/format {:dialect :ansi})))))

(s/defn create-tag :- TagDB
  [data :- Tag]
  (log/info "Creating tag with id" (:tag/id data))
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/insert-into :tag)
                            (helpers/values [data])
                            (helpers/returning :*)
                            (sql/format {:dialect :ansi})))))

(s/defn update-tag :- TagDB
  [{:tag/keys [id] :as data} :- PartialTag]
  (log/info "Updating tag with id" id)
  (let [data (-> data
                 (dissoc :tag/id)
                 (merge {:tag/updated (now)})
                 ;; FIXME: This shouldn't be required. Checkin with HoneySQL for fix.
                 (->> (map (fn [[k v]] [(keyword (name k)) v])) (into {})))]
    (nj/with-transaction [tx db]
      (nj/execute-one! tx (-> (helpers/update :tag)
                              (helpers/set data)
                              (helpers/where [:= :tag/id id])
                              (helpers/returning :*)
                              (sql/format {:dialect :ansi}))))))

(s/defn delete-tag :- TagDB
  [{:tag/keys [id]} :- {:tag/id s/Uuid}]
  (log/info "Deleting tag with id" id)
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/delete-from :tag)
                            (helpers/where [:= :tag/id id])
                            (sql/format {:dialect :ansi})))))
