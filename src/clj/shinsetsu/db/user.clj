(ns shinsetsu.db.user
  (:require
    [next.jdbc :as jdbc]
    [honey.sql :as sql]
    [honey.sql.helpers :as helpers]
    [taoensso.timbre :as log]
    [shinsetsu.db.db :refer [ds]]))

(defn create-user
  [user]
  (jdbc/execute-one! ds (-> (helpers/insert-into :user)
                            (helpers/values [user])
                            (helpers/returning :*)
                            (sql/format {:dialect :ansi}))))

(defn fetch-user-by-username
  [{:user/keys [username]}]
  (log/info "Fetching user with username" username)
  (jdbc/execute-one! ds (-> (helpers/select :*)
                            (helpers/from :user)
                            (helpers/where [:= :user/username username])
                            (sql/format {:dialect :ansi}))))
