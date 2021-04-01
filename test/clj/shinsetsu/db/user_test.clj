(ns shinsetsu.db.user-test
  (:require [clojure.test :refer :all]
            [shinsetsu.db.user :refer :all]
            [shinsetsu.db.core :refer [db]]
            [shinsetsu.config :refer [env]]
            [clojure.test.check.generators :as check-gen]
            [expectations.clojure.test :refer [defexpect expect more in]]
            [puget.printer :refer [pprint]]
            [schema-generators.generators :as g]
            [shinsetsu.schemas :refer :all]
            [user :refer [reset-db migrate]]
            [taoensso.timbre :as log]
            [mount.core :as mount]
            [clojure.data :refer [diff]])
  (:import [java.util Arrays]))

(defn migrate-db-fixture
  [f]
  (log/info "Migrating db")
  (mount/start #'env #'db)
  (migrate)
  (f))

(defn reset-db-fixture
  [f]
  (f)
  (log/info "Resetting db")
  (reset-db))

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
  (let [user       (g/generate User default-leaf-generator)
        user-id    {:user/id (:user/id user)}
        difference (dissoc (g/generate User default-leaf-generator) :user/id)
        new-user   (merge user difference)]
    (expect nil? (read-user user-id))
    (user-compare user (create-user user))
    (user-compare user (read-user user-id))
    (user-compare new-user (update-user new-user))
    (user-compare new-user (read-user user-id))
    (user-compare new-user (delete-user user-id))
    (expect nil? (read-user user-id))))

(defn- current-user-compare
  [expected actual]
  (expect (:user/created actual))
  (expect (:user/updated actual))
  (expect expected (in actual)))

(defexpect current-user-db
  (let [user         (g/generate User default-leaf-generator)
        current-user {:user/id    (:user/id user)
                      :user/token (g/generate NonEmptyContinuousStr)}]
    (create-user user)
    (expect false (check-current-user current-user))
    (current-user-compare current-user (create-current-user current-user))
    (expect (check-current-user current-user))
    (current-user-compare current-user (delete-current-user current-user))
    (expect false (check-current-user current-user))))

(comment
  (create-user (g/generate User default-leaf-generator))
  (create-current-user {:user/id    (java.util.UUID/fromString "a0f7bad9-c730-40f7-9c21-392c938a3b29")
                        :user/token "bao"})
  (require '[eftest.runner :as runner])
  (runner/run-tests [#'shinsetsu.db.user-test/user-db])
  (runner/run-tests [#'shinsetsu.db.user-test/current-user-db]))
