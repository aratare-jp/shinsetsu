(ns shinsetsu.db.tag
  (:require [schema.core :as s]
            [shinsetsu.schemas :refer :all]
            [honey.sql.helpers :as helpers]
            [honey.sql :as sql]
            [puget.printer :refer [pprint]]
            [shinsetsu.db.core :refer :all]
            [taoensso.timbre :as log]
            [shinsetsu.utility :refer :all]))

(s/defn read-tag :- (s/maybe TagDB)
  [{:tag/keys [id]} :- {:tag/id s/Uuid}]
  (log/info "Reading tag with id" id)
  (with-tx-execute-one! (-> (helpers/select :*)
                            (helpers/from :tag)
                            (helpers/where [:= :id id])
                            (sql/format))))

(s/defn read-user-tag :- [TagDB]
  [{:user/keys [id]} :- {:user/id s/Uuid}]
  (log/info "Reading tag with user id" id)
  (with-tx-execute! (-> (helpers/select :*)
                        (helpers/from :tag)
                        (helpers/where [:= :user-id id])
                        (sql/format))))

(s/defn create-tag :- TagDB
  [data :- Tag]
  (log/info "Creating tag with id" (:tag/id data))
  (with-tx-execute-one! (-> (helpers/insert-into :tag)
                            (helpers/values [data])
                            (helpers/returning :*)
                            (sql/format))))

(s/defn update-tag :- TagDB
  [{:tag/keys [id] :as data} :- PartialTag]
  (log/info "Updating tag with id" id)
  (let [data (-> data
                 (dissoc :tag/id)
                 (merge {:tag/updated (now)})
                 ;; FIXME: This shouldn't be required. Checkin with HoneySQL for fix.
                 (->> (map (fn [[k v]] [(keyword (name k)) v])) (into {})))]
    (with-tx-execute-one! (-> (helpers/update :tag)
                              (helpers/set data)
                              (helpers/where [:= :id id])
                              (helpers/returning :*)
                              (sql/format)))))

(s/defn delete-tag :- TagDB
  [{:tag/keys [id]} :- {:tag/id s/Uuid}]
  (log/info "Deleting tag with id" id)
  (with-tx-execute-one! (-> (helpers/delete-from :tag)
                            (helpers/where [:= :id id])
                            (helpers/returning :*)
                            (sql/format))))
