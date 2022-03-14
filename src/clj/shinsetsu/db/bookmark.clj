(ns shinsetsu.db.bookmark
  (:require
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as helpers]
    [honey.sql :as sql]
    [taoensso.timbre :as log]
    [shinsetsu.db.db :refer [ds]]))

(defn create-bookmark
  [bookmark]
  (jdbc/execute-one! ds (-> (helpers/insert-into :bookmark)
                            (helpers/values [bookmark])
                            (helpers/returning :*)
                            (sql/format))))

(defn fetch-bookmarks
  [{tab-id :tab/id user-id :user/id}]
  (jdbc/execute! ds (-> (helpers/select :*)
                        (helpers/from :bookmark)
                        (helpers/where [:= :bookmark/user-id user-id] [:= :bookmark/tab-id tab-id])
                        (sql/format))))
