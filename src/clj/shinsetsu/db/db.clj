(ns shinsetsu.db.db
  (:require
    [mount.core :refer [defstate]]
    [next.jdbc :as jdbc]
    [next.jdbc.date-time]
    [shinsetsu.config :as config]))

(def ads (atom nil))

(defstate ^{:on-reload :noop} ds
  :start
  (reset! ads (jdbc/with-options (jdbc/get-datasource (:db-spec config/env)) jdbc/snake-kebab-opts)))
