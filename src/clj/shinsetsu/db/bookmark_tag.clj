(ns shinsetsu.db.bookmark-tag
  (:require [schema.core :as s]
            [shinsetsu.schemas :refer :all]
            [honey.sql.helpers :as helpers]
            [honey.sql :as sql]
            [puget.printer :refer [pprint]]
            [shinsetsu.db.core :refer :all]
            [taoensso.timbre :as log]
            [shinsetsu.utility :refer :all]))

(s/defn read-bookmark-tag :- [BookmarkTagDB]
  [{:bookmark/keys [id]} :- {:bookmark/id s/Uuid}]
  (log/info "Reading tag with bookmark id" id)
  (with-tx-execute! (-> (helpers/select :*)
                        (helpers/from :bookmark-tag)
                        (helpers/where [:= :bookmark-id id])
                        (sql/format))))

(s/defn create-bookmark-tag :- BookmarkTagDB
  [data :- BookmarkTag]
  (log/info "Creating tag with bookmark id" (:bookmark-tag/bookmark-id data))
  (with-tx-execute-one! (-> (helpers/insert-into :bookmark-tag)
                            (helpers/values [data])
                            (helpers/returning :*)
                            (sql/format))))

(s/defn delete-bookmark-tag :- BookmarkTagDB
  [{:bookmark-tag/keys [bookmark-id tag-id]} :- BookmarkTag]
  (log/info "Deleting tag with bookmark id" bookmark-id)
  (with-tx-execute-one! (-> (helpers/delete-from :bookmark-tag)
                            (helpers/where [:= :bookmark-id bookmark-id] [:= :tag-id tag-id])
                            (helpers/returning :*)
                            (sql/format))))
