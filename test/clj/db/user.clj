(ns db.user
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [clj-test-containers.core :as tc]
    [mount.core :as mount]
    [shinsetsu.db.user :as user-db]
    [next.jdbc :as jdbc]))

(def db-name "fim")
(def db-username "foo")
(def db-password "bar")

(defn setup
  [f]
  (let [container   (-> (tc/create {:image-name    "postgres:latest"
                                    :exposed-ports [5432]
                                    :env-vars      {"POSTGRES_DB"       db-name
                                                    "POSTGRES_USER"     db-username
                                                    "POSTGRES_PASSWORD" db-password}})
                        (tc/bind-filesystem! {:host-path      "/tmp"
                                              :container-path "/opt"
                                              :mode           :read-only})
                        (tc/start!))
        mapped-port (-> container :mapped-ports 5432)
        db-spec     {:classname   "org.postgresql.driver"
                     :subprotocol "postgresql"
                     :subname     (str "//localhost:" mapped-port "/" db-name)
                     :user        db-username
                     :password    db-password}]
    (mount/start-with {#'shinsetsu.db.db/ds (jdbc/get-datasource db-spec)})
    (f)
    (mount/stop)
    (tc/stop! container)))

(use-fixtures :each setup)

(defexpect normal-login
  (expect 1 1))
