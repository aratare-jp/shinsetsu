(ns shinsetsu.db.bookmark
  (:require
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as helpers]
    [honey.sql :as sql]
    [taoensso.timbre :as log]
    [shinsetsu.db.db :refer [ds]]))

(defn create-bookmark
  [{:bookmark/keys [tab-id user-id] :as bookmark}]
  (log/info "Create new bookmark in tab" tab-id "for user" user-id)
  (jdbc/execute-one! ds (-> (helpers/insert-into :bookmark)
                            (helpers/values [bookmark])
                            (helpers/returning :*)
                            (sql/format))))

(defn fetch-bookmark
  [{bookmark-id :bookmark/id user-id :user/id}]
  (log/info "Fetch bookmark" bookmark-id "for user" user-id)
  (jdbc/execute-one! ds (-> (helpers/select :*)
                        (helpers/from :bookmark)
                        (helpers/where [:= :bookmark/user-id user-id] [:= :bookmark/id bookmark-id])
                        (sql/format))))

(defn fetch-bookmarks
  [{tab-id :tab/id user-id :user/id}]
  (log/info "Fetch all bookmarks in tab" tab-id "for user" user-id)
  (jdbc/execute! ds (-> (helpers/select :*)
                        (helpers/from :bookmark)
                        (helpers/where [:= :bookmark/user-id user-id] [:= :bookmark/tab-id tab-id])
                        (sql/format))))
