(ns ^:eftest/synchronized shinsetsu.db.tab-test
  (:require [clojure.test :refer :all]
            [shinsetsu.db.user :refer :all]
            [shinsetsu.db.tab :refer :all]
            [shinsetsu.db.core :as db]
            [shinsetsu.config :refer [env]]
            [expectations.clojure.test :refer [defexpect expect more in]]
            [puget.printer :refer [pprint]]
            [schema-generators.generators :as g]
            [shinsetsu.schemas :refer :all]
            [taoensso.timbre :as log]
            [mount.core :as mount]
            [clojure.data :refer [diff]]
            [schema.core :as s])
  (:import [org.postgresql.util PSQLException]))

(defn migrate-db-fixture
  [f]
  (log/info "Migrating db")
  (mount/start #'env #'db/db #'db/migratus-config)
  (db/migrate)
  (f)
  (mount/stop #'env #'db/db #'db/migratus-config))

(defn reset-db-fixture
  [f]
  (f)
  (log/info "Resetting db")
  (db/reset-db))

(use-fixtures :once migrate-db-fixture)
(use-fixtures :each reset-db-fixture)

(defn- tab-compare
  [expected actual]
  (expect (:tab/created actual))
  (expect (:tab/updated actual))
  (expect expected (in actual)))

(defexpect complete-test
  (testing "Normal path"
    (let [user    (g/generate User default-leaf-generator)
          user-id (:user/id user)]
      (create-user user)
      (doseq [tab (g/sample 50 Tab default-leaf-generator)]
        (let [tab        (merge tab {:tab/user-id user-id})
              tab-id     (:tab/id tab)
              difference (-> (g/generate Tab default-leaf-generator)
                             (dissoc :tab/id)
                             (merge {:tab/user-id user-id}))
              new-tab    (merge tab difference)]
          (expect nil? (read-tab {:tab/id tab-id}))
          (tab-compare tab (create-tab tab))
          (tab-compare tab (read-tab {:tab/id tab-id}))
          (tab-compare new-tab (update-tab new-tab))
          (tab-compare new-tab (read-tab {:tab/id tab-id}))
          (tab-compare new-tab (delete-tab {:tab/id tab-id}))
          (expect nil? (read-tab {:tab/id tab-id})))))))

(defexpect count-tab-test
  (testing "Normal"
    (let [user    (g/generate User default-leaf-generator)
          user-id (:user/id user)
          count   5]
      (create-user user)
      (expect 0 (:count (count-user-tab {:user/id user-id})))
      (doseq [tab (g/sample count Tab default-leaf-generator)]
        (create-tab (merge tab {:tab/user-id user-id})))
      (expect count (:count (count-user-tab {:user/id user-id})))))

  (testing "No user"
    (expect 0 (:count (count-user-tab {:user/id (g/generate s/Uuid)})))))

(defexpect create-tab-test
  (testing "No user"
    (expect PSQLException (create-tab (g/generate Tab default-leaf-generator)))))
