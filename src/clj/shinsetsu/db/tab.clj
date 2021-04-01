(ns shinsetsu.db.tab
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

(s/defn read-tab :- (s/maybe TabDB)
  [{:tab/keys [id]} :- {:tab/id s/Uuid}]
  (log/info "Reading tab with id" id)
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/select :*)
                            (helpers/from :tab)
                            (helpers/where [:= :tab/id id])
                            (sql/format {:dialect :ansi})))))

(s/defn read-user-tab :- [TabDB]
  [{:user/keys [id]} :- {:user/id s/Uuid}]
  (log/info "Reading tabs for user id" id)
  (nj/with-transaction [tx db]
    (nj/execute! tx (-> (helpers/select :*)
                        (helpers/from :tab)
                        (helpers/where [:= :tab/user-id id])
                        (sql/format {:dialect :ansi})))))

(s/defn create-tab :- TabDB
  [data :- Tab]
  (log/info "Creating new tab with id" (:tab/id data))
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/insert-into :tab)
                            (helpers/values [data])
                            (helpers/returning :*)
                            (sql/format {:dialect :ansi})))))

(s/defn update-tab :- TabDB
  [{:tab/keys [id] :as data} :- PartialTab]
  (log/info "Updating tab with id" id)
  (let [data (-> data
                 (dissoc :tab/id)
                 (merge {:tab/updated (now)})
                 ;; FIXME: This shouldn't be required. Checkin with HoneySQL for fix.
                 (->> (map (fn [[k v]] [(keyword (name k)) v])) (into {})))]
    (nj/with-transaction [tx db]
      (nj/execute-one! tx (-> (helpers/update :tab)
                              (helpers/set data)
                              (helpers/where [:= :tab/id id])
                              (helpers/returning :*)
                              (sql/format {:dialect :ansi}))))))

(s/defn delete-tab :- (s/maybe TabDB)
  [{:tab/keys [id]} :- {:tab/id s/Uuid}]
  (log/info "Deleting tab with id" id)
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/delete-from :tab)
                            (helpers/where [:= :tab/id id])
                            (helpers/returning :*)
                            (sql/format {:dialect :ansi})))))
