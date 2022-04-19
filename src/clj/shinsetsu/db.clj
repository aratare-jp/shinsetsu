(ns shinsetsu.db
  (:require
    [mount.core :refer [defstate]]
    [next.jdbc :as jdbc]
    [next.jdbc.date-time]
    [shinsetsu.config :as config]))

(defstate ^{:on-reload :noop} ds
  :start
  (jdbc/with-options (jdbc/get-datasource (:db-spec config/env)) jdbc/snake-kebab-opts))

(comment
  (require '[mount.core :as mount])
  (mount/start ds))
