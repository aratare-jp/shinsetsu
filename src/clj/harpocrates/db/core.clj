(ns harpocrates.db.core
  (:require
    [cheshire.core :refer [generate-string parse-string]]
    [clojure.tools.logging :as log]
    [toucan.db :as db]
    [toucan.models :as models]
    [harpocrates.config :refer [env]]
    [mount.core :refer [defstate]])
  (:import (org.postgresql.util PGobject)
           (clojure.lang IPersistentVector IPersistentMap)
           (java.sql PreparedStatement Array Time Date Timestamp)))

(defstate ^:dynamic *db*
  :start
  (do
    (log/info "Setting default database connection")
    (models/set-root-namespace! 'harpocrates.db)
    (db/set-default-automatically-convert-dashes-and-underscores! true)
    (db/set-default-db-connection! (:database-url env))))

(defn pgobj->clj [^PGobject pgobj]
  (let [type  (.getType pgobj)
        value (.getValue pgobj)]
    (case type
      "json" (parse-string value true)
      "jsonb" (parse-string value true)
      "citext" (str value)
      value)))

(extend-protocol next.jdbc.result-set/ReadableColumn
  Timestamp
  (read-column-by-label [^Timestamp v _]
    (.toLocalDateTime v))
  (read-column-by-index [^Timestamp v _2 _3]
    (.toLocalDateTime v))
  Date
  (read-column-by-label [^Date v _]
    (.toLocalDate v))
  (read-column-by-index [^Date v _2 _3]
    (.toLocalDate v))
  Time
  (read-column-by-label [^Time v _]
    (.toLocalTime v))
  (read-column-by-index [^Time v _2 _3]
    (.toLocalTime v))
  Array
  (read-column-by-label [^Array v _]
    (vec (.getArray v)))
  (read-column-by-index [^Array v _2 _3]
    (vec (.getArray v)))
  PGobject
  (read-column-by-label [^PGobject pgobj _]
    (pgobj->clj pgobj))
  (read-column-by-index [^PGobject pgobj _2 _3]
    (pgobj->clj pgobj)))

(defn clj->jsonb-pgobj [value]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (generate-string value))))

(extend-protocol next.jdbc.prepare/SettableParameter
  IPersistentMap
  (set-parameter [^IPersistentMap v ^PreparedStatement stmt ^long idx]
    (.setObject stmt idx (clj->jsonb-pgobj v)))
  IPersistentVector
  (set-parameter [^IPersistentVector v ^PreparedStatement stmt ^long idx]
    (let [conn      (.getConnection stmt)
          meta      (.getParameterMetaData stmt)
          type-name (.getParameterTypeName meta idx)]
      (if-let [elem-type (when (= (first type-name) \_)
                           (apply str (rest type-name)))]
        (.setObject stmt idx (.createArrayOf conn elem-type (to-array v)))
        (.setObject stmt idx (clj->jsonb-pgobj v))))))
