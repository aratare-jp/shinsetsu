(ns shinsetsu.db.tab
  (:require [schema.core :as s]
            [shinsetsu.schemas :refer :all]
            [honey.sql.helpers :as helpers]
            [honey.sql :as sql]
            [taoensso.timbre :as log]
            [shinsetsu.utility :refer :all]
            [next.jdbc :as jdbc]))

(s/defn read-tab :- (s/maybe TabDB)
  [db :- Transactable
   {:tab/keys [id]} :- {:tab/id s/Uuid}]
  (log/info "Reading tab with id" id)
  (jdbc/execute-one! db (-> (helpers/select :*)
                            (helpers/from :tab)
                            (helpers/where [:= :id id])
                            (sql/format))))

(s/defn read-user-tab :- [TabDB]
  [db :- Transactable
   {:user/keys [id]} :- {:user/id s/Uuid}]
  (log/info "Reading tabs for user id" id)
  (jdbc/execute! db (-> (helpers/select :*)
                        (helpers/from :tab)
                        (helpers/where [:= :user-id id])
                        (sql/format))))

(s/defn count-user-tab :- {:count s/Int}
  [db :- Transactable
   {:user/keys [id]} :- {:user/id s/Uuid}]
  (log/info "Reading tabs for user id" id)
  (jdbc/execute-one! db (-> (helpers/select :%count.*)
                            (helpers/from :tab)
                            (helpers/where [:= :user-id id])
                            (sql/format))))

(s/defn create-tab :- TabDB
  [db :- Transactable
   data :- Tab]
  (log/info "Creating new tab with id" (:tab/id data))
  (jdbc/execute-one! db (-> (helpers/insert-into :tab)
                            (helpers/values [data])
                            (helpers/returning :*)
                            (sql/format))))

(s/defn update-tab :- TabDB
  [db :- Transactable
   {:tab/keys [id] :as data} :- PartialTab]
  (log/info "Updating tab with id" id)
  (let [data (-> data
                 (dissoc :tab/id)
                 (merge {:tab/updated (now)})
                 ;; FIXME: This shouldn't be required. Checkin with HoneySQL for fix.
                 (->> (map (fn [[k v]] [(keyword (name k)) v])) (into {})))]
    (jdbc/execute-one! db (-> (helpers/update :tab)
                              (helpers/set data)
                              (helpers/where [:= :id id])
                              (helpers/returning :*)
                              (sql/format)))))

(s/defn delete-tab :- (s/maybe TabDB)
  [db :- Transactable
   {:tab/keys [id]} :- {:tab/id s/Uuid}]
  (log/info "Deleting tab with id" id)
  (jdbc/execute-one! db (-> (helpers/delete-from :tab)
                            (helpers/where [:= :id id])
                            (helpers/returning :*)
                            (sql/format))))
