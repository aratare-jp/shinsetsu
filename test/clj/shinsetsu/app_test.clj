(ns shinsetsu.app-test
  (:require [clojure.test :refer :all]
            [expectations.clojure.test :refer [defexpect expect more in]]
            [buddy.hashers :as hashers]
            [shinsetsu.db.core :refer [db]]
            [shinsetsu.app :as app]
            [puget.printer :refer [pprint]]
            [ring.mock.request :as mock]
            [mount.core :as mount]
            [shinsetsu.config :refer [env]]
            [shinsetsu.test-utility :refer :all]))

(def db-fixture (get-db-fixture "shinsetsu-app-db"))
(def tdb (:db db-fixture))

(defn each-fixture
  [f]
  (let [db-each-fixture (get-in db-fixture [:fixture :once])]
    (mount/start #'app/app)
    (db-each-fixture f)
    (mount/start #'app/app)))

(use-fixtures :once (get-in db-fixture [:fixture :once]))
(use-fixtures :each each-fixture)

#_(defexpect middleware
    (with-redefs [shinsetsu.db.core/db tdb]
      (let [username "hero@test.com"
            password "awesome"
            mut      `[(shinsetsu.mutations.user/login {:user/username ~username
                                                        :user/password ~password})]
            body     (->transit mut)]
        (let [res (app/app (-> (mock/request :post "/api" body)
                               (mock/content-type "application/transit+json")))]
          (pprint res)
          (expect 201 (:status res))))))

(comment
  (require '[eftest.runner :as efr])
  (import [java.io ByteArrayInputStream ByteArrayOutputStream])
  (require '[cognitect.transit :as transit])
  (do
    (def foo `[(shinsetsu.mutations.user/login {:user/username "foo"
                                                :user/password "bar"})])
    (def out (ByteArrayOutputStream. 4096))
    (def writer (transit/writer out :json))
    (transit/write writer foo)
    (.toString out))
  (efr/run-tests [#'shinsetsu.app-test/middleware]))