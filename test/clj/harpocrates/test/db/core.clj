(ns harpocrates.test.db.core
  (:require
    [harpocrates.db.core :refer [*db*] :as db]
    [java-time.pre-java8]
    [luminus-migrations.core :as migrations]
    [clojure.test :refer :all]
    [next.jdbc :as jdbc]
    [harpocrates.config :refer [env]]
    [mount.core :as mount])
  (:import (java.util UUID)))

(use-fixtures
  :once
  (fn [f]
    (mount/start
      #'harpocrates.config/env
      #'harpocrates.db.core/*db*)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(deftest test-users
  (let [uuid (UUID/randomUUID)]
    (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
      (is (= 1 (db/create-user!
                 t-conn
                 {:id         uuid
                  :first_name "Sam"
                  :last_name  "Smith"
                  :email      "sam.smith@example.com"
                  :password   "pass"})))
      (is (= {:id         uuid
              :first_name "Sam"
              :last_name  "Smith"
              :email      "sam.smith@example.com"
              :password   "pass"
              :last_login nil
              :is_active  nil}
             (db/get-user! t-conn {:id uuid}))))))
