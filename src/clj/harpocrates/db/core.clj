(ns harpocrates.db.core
  (:require [mount.core :refer [defstate]]
            [next.jdbc :as jdbc]
            [next.jdbc.connection :as connection]
            [harpocrates.config :refer [env]])
  (:import [com.zaxxer.hikari HikariDataSource]))

(defstate db
  :start
  (connection/->pool HikariDataSource (select-keys env [:jdbcUrl]))
  :stop
  (.close db))
