(ns shinsetsu.db.core
  (:require [mount.core :refer [defstate]]
            [next.jdbc.connection :as connection]
            [next.jdbc.date-time]
            [next.jdbc :as jdbc]
            [shinsetsu.config :refer [env]]
            [shinsetsu.db.ext])
  (:import [com.zaxxer.hikari HikariDataSource]))

;(defstate db
;  :start
;  (jdbc/with-options
;   (connection/->pool HikariDataSource (select-keys env [:jdbcUrl]))
;    jdbc/snake-kebab-opts)
;  :stop
;  (-> db :connectable .close))
