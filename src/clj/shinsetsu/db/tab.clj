(ns shinsetsu.db.tab
  (:require [schema.core :as s]
            [shinsetsu.schemas :refer :all]
            [next.jdbc :as nj]
            [honey.sql.helpers :as helpers]
            [honey.sql :as sql]
            [puget.printer :refer [pprint]]
            [shinsetsu.db.core :refer :all]
            [taoensso.timbre :as log]
            [shinsetsu.utility :refer :all]
            [schema-generators.generators :as g]))

(s/defn read-tab :- (s/maybe TabDB)
  [{:tab/keys [id]} :- {:tab/id s/Uuid}]
  (log/info "Reading tab with id" id)
  (with-tx-execute-one! (-> (helpers/select :*)
                            (helpers/from :tab)
                            (helpers/where [:= :id id])
                            (sql/format))))

(s/defn read-user-tab :- [TabDB]
  [{:user/keys [id]} :- {:user/id s/Uuid}]
  (log/info "Reading tabs for user id" id)
  (with-tx-execute! (-> (helpers/select :*)
                        (helpers/from :tab)
                        (helpers/where [:= :user-id id])
                        (sql/format))))

(s/defn count-user-tab :- {:count s/Int}
  [{:user/keys [id]} :- {:user/id s/Uuid}]
  (log/info "Reading tabs for user id" id)
  (with-tx-execute-one! (-> (helpers/select :%count.*)
                            (helpers/from :tab)
                            (helpers/where [:= :user-id id])
                            (sql/format))))

(s/defn create-tab :- TabDB
  [data :- Tab]
  (log/info "Creating new tab with id" (:tab/id data))
  (with-tx-execute-one! (-> (helpers/insert-into :tab)
                            (helpers/values [data])
                            (helpers/returning :*)
                            (sql/format))))

(s/defn update-tab :- TabDB
  [{:tab/keys [id] :as data} :- PartialTab]
  (log/info "Updating tab with id" id)
  (let [data (-> data
                 (dissoc :tab/id)
                 (merge {:tab/updated (now)})
                 ;; FIXME: This shouldn't be required. Checkin with HoneySQL for fix.
                 (->> (map (fn [[k v]] [(keyword (name k)) v])) (into {})))]
    (with-tx-execute-one! (-> (helpers/update :tab)
                              (helpers/set data)
                              (helpers/where [:= :id id])
                              (helpers/returning :*)
                              (sql/format)))))

(s/defn delete-tab :- (s/maybe TabDB)
  [{:tab/keys [id]} :- {:tab/id s/Uuid}]
  (log/info "Deleting tab with id" id)
  (with-tx-execute-one! (-> (helpers/delete-from :tab)
                            (helpers/where [:= :id id])
                            (helpers/returning :*)
                            (sql/format))))
