(ns shinsetsu.db.tab-tag
  (:require [schema.core :as s]
            [shinsetsu.schemas :refer :all]
            [honey.sql.helpers :as helpers]
            [honey.sql :as sql]
            [puget.printer :refer [pprint]]
            [shinsetsu.db.core :refer :all]
            [taoensso.timbre :as log]
            [shinsetsu.utility :refer :all]))

(s/defn read-tab-tag :- [TabTagDB]
  [{:tab/keys [id]} :- {:tab/id s/Uuid}]
  (log/info "Reading tag with tab id" id)
  (with-tx-execute! (-> (helpers/select :*)
                        (helpers/from :tab-tag)
                        (helpers/where [:= :tab-id id])
                        (sql/format))))

(s/defn create-tab-tag :- TabTagDB
  [data :- TabTag]
  (log/info "Creating tag with tab id" (:tab-tag/tab-id data))
  (with-tx-execute-one! (-> (helpers/insert-into :tab-tag)
                            (helpers/values [data])
                            (helpers/returning :*)
                            (sql/format))))

(s/defn delete-tab-tag :- (s/maybe TabTagDB)
  [{:tab-tag/keys [tab-id tag-id]} :- TabTag]
  (log/info "Deleting tag with tab id" tab-id)
  (with-tx-execute-one! (-> (helpers/delete-from :tab-tag)
                            (helpers/where [:= :tab-id tab-id] [:= :tag-id tag-id])
                            (helpers/returning :*)
                            (sql/format))))
