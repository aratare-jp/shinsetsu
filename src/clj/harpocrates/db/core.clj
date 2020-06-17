(ns harpocrates.db.core
  (:require
    [cheshire.core :refer [generate-string parse-string]]
    [next.jdbc.date-time]
    [next.jdbc.prepare]
    [next.jdbc.result-set]
    [clojure.tools.logging :as log]
    [conman.core :as conman]
    [harpocrates.config :refer [env]]
    [mount.core :refer [defstate]]
    [camel-snake-kebab.extras :refer [transform-keys]]
    [camel-snake-kebab.core :refer [->kebab-case-keyword]])
  (:import (org.postgresql.util PGobject)
           (clojure.lang IPersistentVector IPersistentMap)
           (java.sql PreparedStatement Array Time Date Timestamp)))

;; Declare here to please the editors. Queries are defined in
;; "resources/sql/queries".
(declare
  create-user!
  update-user!
  get-user!
  get-user-by-email!
  delete-user!
  get-bookmark!
  create-bookmark!
  create-user-bookmark!
  get-bookmark-from-user!)

(defn result-one-snake->kebab
  [this result options]
  (->> (hugsql.adapter/result-one this result options)
       (transform-keys ->kebab-case-keyword)))

(defn result-many-snake->kebab
  [this result options]
  (->> (hugsql.adapter/result-many this result options)
       (transform-keys ->kebab-case-keyword)))

(defn result-affected-snake->kebab
  [this result options]
  (->> (hugsql.adapter/result-affected this result options)
       (transform-keys ->kebab-case-keyword)))

(defn result-raw-snake->kebab
  [this result options]
  (->> (hugsql.adapter/result-raw this result options)
       (transform-keys ->kebab-case-keyword)))

(defmethod hugsql.core/hugsql-result-fn :1 [sym] `result-one-snake->kebab)
(defmethod hugsql.core/hugsql-result-fn :one [sym] `result-one-snake->kebab)
(defmethod hugsql.core/hugsql-result-fn :* [sym] '`result-many-snake->kebab)
(defmethod hugsql.core/hugsql-result-fn :many [sym] `result-many-snake->kebab)
(defmethod hugsql.core/hugsql-result-fn :n [sym] `result-affected-snake->kebab)
(defmethod hugsql.core/hugsql-result-fn :affected [sym] `result-affected-snake->kebab)
(defmethod hugsql.core/hugsql-result-fn :raw [sym] `result-raw-snake->kebab)
(defmethod hugsql.core/hugsql-result-fn :default [sym] `result-raw-snake->kebab)

(defstate ^:dynamic *db*
  :start (if-let [jdbc-url (env :database-url)]
           (conman/connect! {:jdbc-url jdbc-url})
           (do
             (log/warn "database connection URL was not found, please set
             :database-url in your config, e.g: dev-config.edn")
             *db*))
  :stop (conman/disconnect! *db*))

(conman/bind-connection *db* "sql/queries.sql")

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
