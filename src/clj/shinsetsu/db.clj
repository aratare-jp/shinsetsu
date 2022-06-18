(ns shinsetsu.db
  (:require
    [mount.core :refer [defstate]]
    [next.jdbc :as jdbc]
    [next.jdbc.date-time]
    [shinsetsu.config :as config]))

(defstate ^{:on-reload :noop} ds
  :start
  (jdbc/with-options
    (jdbc/get-datasource (merge (:db-spec config/env) {:jdbcUrl (:database-url config/env)}))
    jdbc/snake-kebab-opts))

(comment
  (empty? (select-keys {} [:boo]))
  (require '[mount.core :as mount])
  (mount/start ds))
