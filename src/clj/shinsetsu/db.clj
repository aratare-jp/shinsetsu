(ns shinsetsu.db
  (:require
    [shinsetsu.config :as config]
    [next.jdbc :as jdbc]
    [honey.sql :as sql]
    [mount.core :refer [defstate]]
    [honey.sql.helpers :as helpers]
    [taoensso.timbre :as log]
    [spy.core :as spy]))

(defstate ds
  :start
  (jdbc/get-datasource (:db-spec config/env)))

(defn create-user
  [user]
  (try
    (jdbc/execute-one! ds (-> (helpers/insert-into :user)
                              (helpers/values [user])
                              (helpers/returning :*)
                              (sql/format {:dialect :ansi})))
    (catch Exception e
      (log/error e))))

(defn fetch-user-by-username
  [{:user/keys [username]}]
  (log/info "Fetching user with username" username)
  (try
    (jdbc/execute-one! ds (-> (helpers/select :*)
                              (helpers/from :user)
                              (helpers/where [:= :user/username username])
                              (sql/format {:dialect :ansi})))
    (catch Exception e
      (log/error e))))

(defn create-tab
  [tab]
  (try
    (jdbc/execute-one! ds (-> (helpers/insert-into :tab)
                              (helpers/values [tab])
                              (helpers/returning :*)
                              (sql/format)))
    (catch Exception e
      (log/error e))))

(defn fetch-tabs
  [{user-id :user/id}]
  (try
    (jdbc/execute! ds (-> (helpers/select :*)
                          (helpers/from :tab)
                          (helpers/where [:= :tab/user-id user-id])
                          (sql/format)))
    (catch Exception e
      (log/error e))))

(defn fetch-tab
  [{tab-id :tab/id user-id :user/id}]
  (try
    (jdbc/execute-one! ds (-> (helpers/select :*)
                              (helpers/from :tab)
                              (helpers/where [:= :tab/user-id user-id] [:= :tab/id tab-id])
                              (sql/format)))
    (catch Exception e
      (log/error e))))


(defn create-bookmark
  [bookmark]
  (try
    (jdbc/execute-one! ds (-> (helpers/insert-into :bookmark)
                              (helpers/values [bookmark])
                              (helpers/returning :*)
                              (sql/format)))
    (catch Exception e
      (log/error e))))

(defn fetch-bookmarks
  [{tab-id :tab/id user-id :user/id}]
  (try
    (jdbc/execute! ds (-> (helpers/select :*)
                          (helpers/from :bookmark)
                          (helpers/where [:= :bookmark/user-id user-id] [:= :bookmark/tab-id tab-id])
                          (sql/format)))
    (catch Exception e
      (log/error e))))

(comment
  (user/start)
  (user/restart)
  (require '[shinsetsu.db :as db])
  #_(let [user     (db/fetch-user-by-username {:user/username "asdf"})
          user-id  (:user/id user)
          tab      (db/fetch-tabs user-id)
          bookmark (db/create-bookmark {:bookmark/title  "Foo"
                                        :bookmark/url    "Bar"
                                        :user/user-id    user-id
                                        :bookmark/tab-id (java.util.UUID/fromString "376639bf-5af6-48ee-8d6f-1f1c5d0be0a3")})]
      bookmark)
  #_(let [user     (db/fetch-user-by-username {:user/username "asdf"})
          user-id  (:user/id user)
          tab      (db/fetch-tabs user-id)
          bookmark (db/fetch-bookmarks {:user/id user-id
                                        :tab/id  (java.util.UUID/fromString "376639bf-5af6-48ee-8d6f-1f1c5d0be0a3")})]
      bookmark))
