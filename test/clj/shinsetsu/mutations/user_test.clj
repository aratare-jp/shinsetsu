(ns shinsetsu.mutations.user-test
  (:require [clojure.test :refer :all]
            [expectations.clojure.test :refer [defexpect expect more in]]
            [buddy.hashers :as hashers]
            [shinsetsu.db.core :refer [db]]
            [shinsetsu.db.user :refer :all]
            [shinsetsu.schemas :refer :all]
            [puget.printer :refer [pprint]]
            [shinsetsu.config :refer [env]]
            [shinsetsu.test-utility :refer :all]
            [shinsetsu.parser :refer [pathom-parser]]
            [schema-generators.generators :as g]
            [schema.core :as s]
            [mount.core :as mount]))

(def db-fixture (get-db-fixture "shinsetsu-app-db"))
(def tdb (:db db-fixture))

(defn each-fixture
  [f]
  (let [db-each-fixture (get-in db-fixture [:fixture :once])]
    (mount/start #'shinsetsu.config/env #'shinsetsu.parser/pathom-parser)
    (db-each-fixture f)
    (mount/stop #'shinsetsu.config/env #'shinsetsu.parser/pathom-parser)))

(use-fixtures :once (get-in db-fixture [:fixture :once]))
(use-fixtures :each each-fixture)

(defexpect user-test
  (with-redefs [shinsetsu.db.core/db tdb
                shinsetsu.config/env (merge shinsetsu.config/env
                                            {:secret (g/generate NonEmptyContinuousStr)})]
    (let [user     (g/generate User default-leaf-generator)
          username (:user/username user)
          password (:user/password user)
          mut      `[(shinsetsu.mutations.user/login {:user/username ~username
                                                      :user/password ~password})]
          body     (->transit mut)]
      (create-user shinsetsu.db.core/db (update user :user/password hashers/derive))
      (let [expected {'shinsetsu.mutations.user/login {:user/id     (:user/id user)
                                                       :user/valid? true}}]
        (expect expected (pathom-parser {} mut))))))

(comment
  (do
    (mount.core/stop #'shinsetsu.config/env)
    (mount.core/start #'shinsetsu.config/env))
  (do
    (require '[eftest.runner :as efr])
    (import [java.io ByteArrayInputStream ByteArrayOutputStream])
    (require '[cognitect.transit :as transit])
    (require '[clojure.tools.namespace.repl :refer [refresh]]))
  (refresh)
  (efr/run-tests [#'shinsetsu.mutations.user-test/user-test]))