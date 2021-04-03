(ns shinsetsu.db.bookmark-tag
  (:require [schema.core :as s]
            [shinsetsu.schemas :refer :all]
            [honey.sql.helpers :as helpers]
            [honey.sql :as sql]
            [taoensso.timbre :as log]
            [shinsetsu.utility :refer :all]
            [next.jdbc :as jdbc]))

(s/defn read-bookmark-tag :- [BookmarkTagDB]
  [db :- Transactable
   {:bookmark/keys [id]} :- {:bookmark/id s/Uuid}]
  (log/info "Reading tag with bookmark id" id)
  (jdbc/execute! db (-> (helpers/select :*)
                        (helpers/from :bookmark-tag)
                        (helpers/where [:= :bookmark-id id])
                        (sql/format))))

(s/defn create-bookmark-tag :- BookmarkTagDB
  [db :- Transactable
   data :- BookmarkTag]
  (log/info "Creating tag with bookmark id" (:bookmark-tag/bookmark-id data))
  (jdbc/execute-one! db (-> (helpers/insert-into :bookmark-tag)
                            (helpers/values [data])
                            (helpers/returning :*)
                            (sql/format))))

(s/defn delete-bookmark-tag :- BookmarkTagDB
  [db :- Transactable
   {:bookmark-tag/keys [bookmark-id tag-id]} :- BookmarkTag]
  (log/info "Deleting tag with bookmark id" bookmark-id)
  (jdbc/execute-one! db (-> (helpers/delete-from :bookmark-tag)
                            (helpers/where [:= :bookmark-id bookmark-id] [:= :tag-id tag-id])
                            (helpers/returning :*)
                            (sql/format))))
