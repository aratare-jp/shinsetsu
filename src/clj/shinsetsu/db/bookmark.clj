(ns shinsetsu.db.bookmark
  (:require [schema.core :as s]
            [shinsetsu.schemas :refer :all]
            [honey.sql.helpers :as helpers]
            [honey.sql :as sql]
            [taoensso.timbre :as log]
            [shinsetsu.utility :refer :all]
            [next.jdbc :as jdbc]))

(s/defn read-bookmark :- (s/maybe BookmarkDB)
  [db :- Transactable
   {:bookmark/keys [id]} :- {:bookmark/id s/Uuid}]
  (log/info "Reading bookmark with id" id)
  (jdbc/execute-one! db (-> (helpers/select :*)
                            (helpers/from :bookmark)
                            (helpers/where [:= :id id])
                            (sql/format))))

(s/defn read-tab-bookmark :- [BookmarkDB]
  [db :- Transactable
   {:tab/keys [id]} :- {:tab/id s/Uuid}]
  (log/info "Reading bookmark with tab id" id)
  (jdbc/execute! db (-> (helpers/select :*)
                        (helpers/from :bookmark)
                        (helpers/where [:= :tab-id id])
                        (sql/format))))

(s/defn create-bookmark :- BookmarkDB
  [db :- Transactable
   data :- Bookmark]
  (log/info "Creating bookmark with id" (:bookmark/id data))
  (jdbc/execute-one! db (-> (helpers/insert-into :bookmark)
                            (helpers/values [data])
                            (helpers/returning :*)
                            (sql/format))))

(s/defn update-bookmark :- BookmarkDB
  [db :- Transactable
   {:bookmark/keys [id] :as data} :- PartialBookmark]
  (log/info "Updating bookmark with id" (:bookmark/id data))
  (let [data (-> data
                 (dissoc :bookmark/id)
                 (merge {:bookmark/updated (now)})
                 ;; FIXME: This shouldn't be required. Checkin with HoneySQL for fix.
                 (->> (map (fn [[k v]] [(keyword (name k)) v])) (into {})))]
    (jdbc/execute-one! db (-> (helpers/update :bookmark)
                              (helpers/set data)
                              (helpers/where [:= :id id])
                              (helpers/returning :*)
                              (sql/format)))))

(s/defn delete-bookmark :- (s/maybe BookmarkDB)
  [db :- Transactable
   {:bookmark/keys [id]} :- {:bookmark/id s/Uuid}]
  (log/info "Deleting bookmark with id" id)
  (jdbc/execute-one! db (-> (helpers/delete-from :bookmark)
                            (helpers/where [:= :id id])
                            (helpers/returning :*)
                            (sql/format))))
