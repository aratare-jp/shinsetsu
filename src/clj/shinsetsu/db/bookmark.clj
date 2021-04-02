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

(s/defn read-tag-bookmark :- [BookmarkDB]
  [{:tag/keys [id]} :- {:tag/id s/Uuid}]
  (log/info "Reading bookmark with tag id" id)
  (with-tx-execute-one! (-> (helpers/select :*)
                            (helpers/from :bookmark)
                            (helpers/where [:= :tag-id id])
                            (sql/format))))

(s/defn create-bookmark :- Bookmark
  [data :- Bookmark]
  (log/info "Creating bookmark with id" (:bookmark/id data))
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/insert-into :bookmark)
                            (helpers/values [data])
                            (helpers/returning :*)
                            (sql/format {:dialect :ansi})))))

(s/defn update-bookmark :- Bookmark
  [{:bookmark/keys [id] :as data} :- PartialBookmark]
  (log/info "Updating bookmark with id" (:bookmark/id data))
  (let [data (-> data
                 (dissoc :bookmark/id)
                 (merge {:bookmark/updated (now)})
                 ;; FIXME: This shouldn't be required. Checkin with HoneySQL for fix.
                 (->> (map (fn [[k v]] [(keyword (name k)) v])) (into {})))]
    (nj/with-transaction [tx db]
      (nj/execute-one! tx (-> (helpers/update :bookmark)
                              (helpers/set data)
                              (helpers/where [:= :bookmark/id id])
                              (helpers/returning :*)
                              (sql/format {:dialect :ansi}))))))

(s/defn delete-bookmark :- (s/maybe Bookmark)
  [{:bookmark/keys [id]} :- {:bookmark/id s/Uuid}]
  (log/info "Deleting bookmark with id" id)
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/delete-from :bookmark)
                            (helpers/where [:= :bookmark/id id])
                            (helpers/returning :*)
                            (sql/format {:dialect :ansi})))))

(s/defn read-tab-bookmark :- [BookmarkDB]
  [{:tab/keys [id]} :- {:tab/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/select :bookmark)
                            (helpers/where [:= :bookmark/tab-id id])
                            (helpers/returning :*)
                            (sql/format {:dialect :ansi})))))

(s/defn read-bookmark-tag :- [Tag]
  [{:bookmark/keys [id]} :- {:bookmark/id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute! tx (-> (helpers/select :bookmark-tag)
                        (helpers/where [:= :bookmark-id id])
                        (sql/format {:dialect :ansi})))))

(s/defn create-bookmark-tag :- {:bookmark/id s/Uuid :tag-id s/Uuid}
  [data :- {:bookmark/id s/Uuid :tag-id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/insert-into :tab-tag)
                            (helpers/values [data])
                            (sql/format {:dialect :ansi})))))

(s/defn delete-bookmark-tag :- {:bookmark/id s/Uuid :tag-id s/Uuid}
  [{:bookmark/keys [bookmark-id] :tag/keys [tag-id]} :- {:bookmark/id s/Uuid :tag-id s/Uuid}]
  (nj/with-transaction [tx db]
    (nj/execute-one! tx (-> (helpers/delete-from :tab-tag)
                            (helpers/where [:= :tag-id tag-id] [:= :bookmark-id bookmark-id])
                            (sql/format {:dialect :ansi})))))