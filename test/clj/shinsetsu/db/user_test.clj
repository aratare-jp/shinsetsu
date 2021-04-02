(ns shinsetsu.db.user-test
  (:require [clojure.test :refer :all]
            [shinsetsu.db.user :refer :all]
            [shinsetsu.db.core :as db]
            [shinsetsu.config :refer [env]]
            [expectations.clojure.test :refer [defexpect expect more in]]
            [puget.printer :refer [pprint]]
            [schema-generators.generators :as g]
            [shinsetsu.schemas :refer :all]
            [taoensso.timbre :as log]
            [mount.core :as mount]
            [clojure.data :refer [diff]])
  (:import [java.util Arrays]))

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

(defn- user-compare
  [expected actual]
  (expect (:user/created actual))
  (expect (:user/updated actual))
  (let [image          (bytes (:user/image actual))
        expected-image (bytes (:user/image expected))
        actual         (dissoc actual :user/image)
        expected       (dissoc expected :user/image)]
    (expect expected (in actual))
    (expect (Arrays/equals image expected-image))))

(defexpect user-db
  (doseq [user (g/sample 50 User default-leaf-generator)]
    (let [user-id    {:user/id (:user/id user)}
          difference (dissoc (g/generate User default-leaf-generator) :user/id)
          new-user   (merge user difference)]
      (expect nil? (read-user user-id))
      (user-compare user (create-user user))
      (user-compare user (read-user user-id))
      (user-compare new-user (update-user new-user))
      (user-compare new-user (read-user user-id))
      (user-compare new-user (delete-user user-id))
      (expect nil? (read-user user-id)))))
