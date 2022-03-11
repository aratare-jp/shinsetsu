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
  [user-id]
  (try
    (jdbc/execute! ds (-> (helpers/select :*)
                          (helpers/from :tab)
                          (helpers/where [:= :tab/user-id user-id])
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

(comment
  (user/restart)
  (user/start)
  (clojure.repl/doc tap>)
  (require '[shinsetsu.db :as db])
  (as-> (db/fetch-user-by-username {:user/username "asdf"}) user
        (:user/id user)
        #_(db/fetch-tabs user)
        (db/create-tab {:tab/name "Bay" :tab/user-id user})))
