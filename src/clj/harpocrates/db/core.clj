(ns harpocrates.db.core
  (:require [mount.core :refer [defstate]]
            [next.jdbc.connection :as connection]
            [harpocrates.config :refer [env]]
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

(defmacro crud-fns
  "Create CRUD db functions.

  E.g. (crud-fns user User User?)"
  [type-name t]
  (let [opt-type    (symbol (str (name t) "?"))
        read-fn     (symbol (str "read-" (name type-name)))
        create-fn   (symbol (str "create-" (name type-name)))
        update-fn   (symbol (str "update-" (name type-name)))
        delete-fn   (symbol (str "delete-" (name type-name)))
        type-keys   (keyword (name type-name) "keys")
        type-id-key (keyword (name type-name) "id")
        type-key    (keyword (name type-name))
        id          (gensym)
        data        (gensym)
        tx          (gensym)
        k           (gensym)
        v           (gensym)]
    `(do
       (def ~opt-type
         (into {} (map (fn [[~k ~v]] [(s/optional-key ~k) (s/maybe ~v)]) ~t)))

       (s/defn ~read-fn
         [{~type-keys [~id]} :- {~type-id-key s/Uuid}]
         (nj/with-transaction [~tx db]
           (nj/execute-one! ~tx (log/spy :info (-> (helpers/select :*)
                                                   (helpers/from ~type-key)
                                                   (helpers/where [:= ~type-id-key ~id])
                                                   (sql/format :quoting :ansi))))))

       (s/defn ~create-fn
         [~data :- ~t]
         (nj/with-transaction [~tx db]
           (nj/execute-one! ~tx (log/spy :info (-> (helpers/insert-into ~type-key)
                                                   (helpers/values [~data])
                                                   (sql/format :quoting :ansi))))))

       (s/defn ~update-fn
         [{~type-keys [~id] :as ~data} :- ~opt-type]
         (nj/with-transaction [~tx db]
           (nj/execute-one! ~tx (-> (helpers/update ~type-key)
                                    (helpers/sset ~data)
                                    (helpers/where [:= ~type-id-key ~id])
                                    (sql/format :quoting :ansi)))))

       (s/defn ~delete-fn
         [{~type-keys [~id]} :- {~type-id-key s/Uuid}]
         (nj/with-transaction [~tx db]
           (nj/execute-one! ~tx (-> (helpers/delete-from ~type-key)
                                    (helpers/where [:= ~type-id-key ~id])
                                    (sql/format :quoting :ansi))))))))
