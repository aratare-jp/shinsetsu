(ns shinsetsu.db.core
  (:require [mount.core :refer [defstate]]
            [next.jdbc.connection :as connection]
            [shinsetsu.config :refer [env]]
            [schema.core :as s]
            [next.jdbc :as nj]
            [honeysql.core :as sql]
            [honeysql.helpers :as helpers]
            [taoensso.timbre :as log]
            [next.jdbc.date-time])
  (:import [com.zaxxer.hikari HikariDataSource]))

(defstate db
  :start
  (connection/->pool HikariDataSource (select-keys env [:jdbcUrl]))
  :stop
  (.close db))
