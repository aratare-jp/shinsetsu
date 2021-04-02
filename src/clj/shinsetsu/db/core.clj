(ns shinsetsu.db.core
  (:require [mount.core :refer [defstate]]
            [next.jdbc.connection :as connection]
            [shinsetsu.config :refer [env]]
            [next.jdbc.date-time]
            [cheshire.core :refer [generate-string parse-string]]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [schema.core :as s]
            [migratus.core :as migratus])
  (:import [com.zaxxer.hikari HikariDataSource]
           [org.postgresql.util PGobject]
           [java.sql Timestamp Date Time Array PreparedStatement]
           [clojure.lang IPersistentMap IPersistentVector]
           [java.time OffsetDateTime ZoneOffset]))

(defstate db
  :start
  (jdbc/with-options
    (connection/->pool HikariDataSource (select-keys env [:jdbcUrl]))
    jdbc/snake-kebab-opts)
  :stop
  (-> db :connectable .close))

(defstate migratus-config
  :start
  {:store         :database
   :migration-dir "migrations/"
   :db            {:datasource (:connectable db)}})

(defn with-tx-execute!
  "`execute!` the given query inside a tx. This function properly handles wrapping of the underlying
  transaction with proper `builder-fn`"
  [query]
  (jdbc/with-transaction [tx db]
    (let [tx (jdbc/with-options tx jdbc/snake-kebab-opts)]
      (jdbc/execute! tx query))))

(defn with-tx-execute-one!
  "`execute-one!` the given query inside a tx. This function properly handles wrapping of the
  underlying transaction with proper `builder-fn`"
  [query]
  (jdbc/with-transaction [tx db]
    (let [tx (jdbc/with-options tx jdbc/snake-kebab-opts)]
      (jdbc/execute-one! tx query))))

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
    (-> v .toInstant (OffsetDateTime/ofInstant ZoneOffset/UTC)))
  (read-column-by-index [^Timestamp v _2 _3]
    (-> v .toInstant (OffsetDateTime/ofInstant ZoneOffset/UTC)))
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
  OffsetDateTime
  (set-parameter [^OffsetDateTime v ^PreparedStatement stmt ^long idx]
    (.setObject stmt idx (-> v (.withOffsetSameInstant ZoneOffset/UTC) .toInstant Timestamp/from)))
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

(comment
  (.toString (java.time.OffsetDateTime/now))
  (-> (java.time.OffsetDateTime/now)
      (.withOffsetSameInstant java.time.ZoneOffset/UTC)
      .toInstant
      Timestamp/from)
  (let [now (java.time.OffsetDateTime/now)]
    (println (-> now
                 (.withOffsetSameInstant java.time.ZoneOffset/UTC)
                 .toInstant
                 Timestamp/from
                 .toInstant
                 (OffsetDateTime/ofInstant ZoneOffset/UTC)
                 .toString)))
  (let [now (java.time.OffsetDateTime/now)]
    (println (-> now
                 (.withOffsetSameInstant java.time.ZoneOffset/UTC)
                 .toInstant
                 Timestamp/from
                 .toString)))
  (-> (java.time.OffsetDateTime/now)
      .toInstant
      Timestamp/from))


(defn reset-db
  "Resets database."
  []
  (migratus/reset migratus-config))

(defn migrate
  "Migrates database up for all outstanding migrations."
  []
  (migratus/migrate migratus-config))

(defn rollback
  "Rollback latest database migration."
  []
  (migratus/rollback migratus-config))

(s/defn up
  "Bring up migrations matching the given ids"
  [& ids :- [s/Str]]
  (apply migratus/up migratus-config ids))

(s/defn down
  "Bring down migrations matching the given ids"
  [& ids :- [s/Str]]
  (apply migratus/down migratus-config ids))

(s/defn create-migration
  "Create a new migration instruction"
  [name :- s/Str]
  (migratus/create migratus-config name))