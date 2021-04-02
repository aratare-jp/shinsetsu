(ns shinsetsu.db.bookmark
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

(s/defn read-bookmark :- (s/maybe BookmarkDB)
  [{:bookmark/keys [id]} :- {:bookmark/id s/Uuid}]
  (log/info "Reading bookmark with id" id)
  (with-tx-execute-one! (-> (helpers/select :*)
                            (helpers/from :bookmark)
                            (helpers/where [:= :id id])
                            (sql/format))))

(s/defn read-tab-bookmark :- [BookmarkDB]
  [{:tab/keys [id]} :- {:tab/id s/Uuid}]
  (log/info "Reading bookmark with tab id" id)
  (with-tx-execute-one! (-> (helpers/select :*)
                            (helpers/from :bookmark)
                            (helpers/where [:= :tab-id id])
                            (sql/format))))

(s/defn create-bookmark :- BookmarkDB
  [data :- Bookmark]
  (log/info "Creating bookmark with id" (:bookmark/id data))
  (with-tx-execute-one! (-> (helpers/insert-into :bookmark)
                            (helpers/values [data])
                            (helpers/returning :*)
                            (sql/format))))

(s/defn update-bookmark :- BookmarkDB
  [{:bookmark/keys [id] :as data} :- PartialBookmark]
  (log/info "Updating bookmark with id" (:bookmark/id data))
  (let [data (-> data
                 (dissoc :bookmark/id)
                 (merge {:bookmark/updated (now)})
                 ;; FIXME: This shouldn't be required. Checkin with HoneySQL for fix.
                 (->> (map (fn [[k v]] [(keyword (name k)) v])) (into {})))]
    (with-tx-execute-one! (-> (helpers/update :bookmark)
                              (helpers/set data)
                              (helpers/where [:= :id id])
                              (helpers/returning :*)
                              (sql/format)))))

(s/defn delete-bookmark :- (s/maybe BookmarkDB)
  [{:bookmark/keys [id]} :- {:bookmark/id s/Uuid}]
  (log/info "Deleting bookmark with id" id)
  (with-tx-execute-one! (-> (helpers/delete-from :bookmark)
                            (helpers/where [:= :id id])
                            (helpers/returning :*)
                            (sql/format))))
