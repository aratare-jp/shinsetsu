(ns shinsetsu.db.user-test
  (:require [clojure.test :refer :all]
            [shinsetsu.db.user :refer :all]
            [expectations.clojure.test :refer [defexpect expect more in]]
            [schema-generators.generators :as g]
            [shinsetsu.schemas :refer :all]
            [clojure.data :refer [diff]]
            [shinsetsu.db.utility :refer :all])
  (:import [java.util Arrays]))

(def db-fixture (get-db-fixture "shinsetsu-user-db"))
(def db (:db db-fixture))
(use-fixtures :once (get-in db-fixture [:fixture :once]))
(use-fixtures :each (get-in db-fixture [:fixture :each]))

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
      (expect nil? (read-user db user-id))
      (user-compare user (create-user db user))
      (user-compare user (read-user db user-id))
      (user-compare new-user (update-user db new-user))
      (user-compare new-user (read-user db user-id))
      (user-compare new-user (delete-user db user-id))
      (expect nil? (read-user db user-id)))))
