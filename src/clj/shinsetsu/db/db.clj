(ns shinsetsu.db.db
  (:require
    [mount.core :refer [defstate]]
    [next.jdbc :as jdbc]
    [shinsetsu.config :as config]))

(defstate ds
  :start
  (jdbc/get-datasource (:db-spec config/env)))
