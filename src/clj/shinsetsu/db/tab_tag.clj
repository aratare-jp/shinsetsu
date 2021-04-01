(ns shinsetsu.db.tab-tag
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

(s/defn read-tab-tag :- [TabTagDB]
  [{:tab/keys [id]} :- {:tab/id s/Uuid}]
  (log/info "Reading tag with tab id" id)
  (doall (map #(simplify-kw % "tab-tag")
              (nj/with-transaction [tx db]
                (nj/execute! tx (-> (helpers/select :*)
                                    (helpers/from :tab_tag)
                                    (helpers/where [:= :tab-id id])
                                    (sql/format {:dialect :ansi})))))))

(s/defn create-tab-tag :- TabTagDB
  [data :- TagDB]
  (log/info "Creating tag with tab id" (:tab-tag/tab-id data))
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/insert-into :tab-tag)
                            (helpers/values [data])
                            (helpers/returning :*)
                            (sql/format {:dialect :ansi})))))

(s/defn delete-tab-tag :- (s/maybe TabTagDB)
  [{:tab-tag/keys [tab-id tag-id]} :- TabTag]
  (log/info "Deleting tag with tab id" tab-id)
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/delete-from :tab_tag)
                            (helpers/where [:= :tab-id tab-id] [:= :tag-id tag-id])
                            (helpers/returning :*)
                            (sql/format {:dialect :ansi})))))
