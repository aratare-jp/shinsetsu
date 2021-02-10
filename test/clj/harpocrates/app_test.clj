(ns harpocrates.app-test
  (:require [clojure.test :refer :all]
            [buddy.hashers :as hashers]
            [harpocrates.app :as app]
            [harpocrates.db.core :as db]
            [puget.printer :refer [pprint]]
            [ring.mock.request :as mock]
            [mount.core :as mount]
            [harpocrates.config :refer [env]]))

(defn app-fixture
  [f]
  (mount/start #'app/app #'env)
  (f)
  (mount/stop #'app/app #'env))

(defn db-fixture
  [f]
  (mount/start #'db/*db*)
  (f)
  (mount/stop #'db/*db*))

(use-fixtures :once app-fixture)
(use-fixtures :each db-fixture)

(deftest middleware
  (let [username "hero@test.com"
        password "awesome"]
    (let [res (app/app (mock/request :post "/api" "[(harpocrates.mutations.auth/login {:username hero@test.com
                                                                                       :password awesome})]"))]
      (pprint res)
      (is (= (:status res) 200))
      (is (-> res :body :token)))))