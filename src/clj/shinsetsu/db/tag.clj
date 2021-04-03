(ns shinsetsu.db.tag
  (:require [schema.core :as s]
            [shinsetsu.schemas :refer :all]
            [honey.sql.helpers :as helpers]
            [honey.sql :as sql]
            [taoensso.timbre :as log]
            [shinsetsu.utility :refer :all]
            [next.jdbc :as jdbc]))

(s/defn read-tag :- (s/maybe TagDB)
  [db :- Transactable
   {:tag/keys [id]} :- {:tag/id s/Uuid}]
  (log/info "Reading tag with id" id)
  (jdbc/execute-one! db (-> (helpers/select :*)
                            (helpers/from :tag)
                            (helpers/where [:= :id id])
                            (sql/format))))

(s/defn read-user-tag :- [TagDB]
  [db :- Transactable
   {:user/keys [id]} :- {:user/id s/Uuid}]
  (log/info "Reading tag with user id" id)
  (jdbc/execute! db (-> (helpers/select :*)
                        (helpers/from :tag)
                        (helpers/where [:= :user-id id])
                        (sql/format))))

(s/defn create-tag :- TagDB
  [db :- Transactable
   data :- Tag]
  (log/info "Creating tag with id" (:tag/id data))
  (jdbc/execute-one! db (-> (helpers/insert-into :tag)
                            (helpers/values [data])
                            (helpers/returning :*)
                            (sql/format))))

(s/defn update-tag :- TagDB
  [db :- Transactable
   {:tag/keys [id] :as data} :- PartialTag]
  (log/info "Updating tag with id" id)
  (let [data (-> data
                 (dissoc :tag/id)
                 (merge {:tag/updated (now)})
                 ;; FIXME: This shouldn't be required. Checkin with HoneySQL for fix.
                 (->> (map (fn [[k v]] [(keyword (name k)) v])) (into {})))]
    (jdbc/execute-one! db (-> (helpers/update :tag)
                              (helpers/set data)
                              (helpers/where [:= :id id])
                              (helpers/returning :*)
                              (sql/format)))))

(s/defn delete-tag :- TagDB
  [db :- Transactable
   {:tag/keys [id]} :- {:tag/id s/Uuid}]
  (log/info "Deleting tag with id" id)
  (jdbc/execute-one! db (-> (helpers/delete-from :tag)
                            (helpers/where [:= :id id])
                            (helpers/returning :*)
                            (sql/format))))
