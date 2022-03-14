(ns shinsetsu.db.tab
  (:require
    [shinsetsu.db.db :refer [ds]]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as helpers]
    [honey.sql :as sql]
    [taoensso.timbre :as log]))

(defn create-tab
  [tab]
  (jdbc/execute-one! ds (-> (helpers/insert-into :tab)
                            (helpers/values [tab])
                            (helpers/returning :*)
                            (sql/format))))

(defn fetch-tabs
  [{user-id :user/id}]
  (jdbc/execute! ds (-> (helpers/select :*)
                        (helpers/from :tab)
                        (helpers/where [:= :tab/user-id user-id])
                        (sql/format))))

(defn fetch-tab
  [{tab-id :tab/id user-id :user/id}]
  (jdbc/execute-one! ds (-> (helpers/select :*)
                            (helpers/from :tab)
                            (helpers/where [:= :tab/user-id user-id] [:= :tab/id tab-id])
                            (sql/format))))
