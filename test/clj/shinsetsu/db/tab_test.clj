(ns shinsetsu.db.tab-test
  (:require [clojure.test :refer :all]
            [shinsetsu.db.user :refer :all]
            [shinsetsu.db.tab :refer :all]
            [shinsetsu.test-utility :refer :all]
            [shinsetsu.config :refer [env]]
            [expectations.clojure.test :refer [defexpect expect more in]]
            [schema-generators.generators :as g]
            [shinsetsu.schemas :refer :all]
            [clojure.data :refer [diff]]
            [schema.core :as s])
  (:import [org.postgresql.util PSQLException]))

(def db-fixture (get-db-fixture "shinsetsu-tab-db"))
(def db (:db db-fixture))
(use-fixtures :once (get-in db-fixture [:fixture :once]))
(use-fixtures :each (get-in db-fixture [:fixture :each]))

(defn- tab-compare
  [expected actual]
  (expect (:tab/created actual))
  (expect (:tab/updated actual))
  (expect expected (in actual)))

(defexpect complete-test
  (testing "Normal path"
    (let [user    (create-user db (g/generate User default-leaf-generator))
          user-id (:user/id user)]
      (doseq [tab (g/sample 50 Tab default-leaf-generator)]
        (let [tab        (merge tab {:tab/user-id user-id})
              tab-id     (:tab/id tab)
              difference (-> (g/generate Tab default-leaf-generator)
                             (dissoc :tab/id)
                             (merge {:tab/user-id user-id}))
              new-tab    (merge tab difference)]
          (expect nil? (read-tab db {:tab/id tab-id}))
          (tab-compare tab (create-tab db tab))
          (tab-compare tab (read-tab db {:tab/id tab-id}))
          (tab-compare new-tab (update-tab db new-tab))
          (tab-compare new-tab (read-tab db {:tab/id tab-id}))
          (tab-compare new-tab (delete-tab db {:tab/id tab-id}))
          (expect nil? (read-tab db {:tab/id tab-id})))))))

(defexpect count-tab-test
  (testing "Normal"
    (let [user    (g/generate User default-leaf-generator)
          user-id (:user/id user)
          count   5]
      (create-user db user)
      (expect 0 (:count (count-user-tab db {:user/id user-id})))
      (doseq [tab (g/sample count Tab default-leaf-generator)]
        (create-tab db (merge tab {:tab/user-id user-id})))
      (expect count (:count (count-user-tab db {:user/id user-id})))))

  (testing "No user"
    (expect 0 (:count (count-user-tab db {:user/id (g/generate s/Uuid)})))))

(defexpect create-tab-test
  (testing "No user"
    (expect PSQLException (create-tab db (g/generate Tab default-leaf-generator)))))
