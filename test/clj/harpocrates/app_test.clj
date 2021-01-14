(ns harpocrates.app-test
  (:require [clojure.test :refer :all]
            [buddy.hashers :as hashers]
            [harpocrates.app :as app]
            [harpocrates.db.core :as db]
            [puget.printer :refer [pprint]]
            [ring.mock.request :as mock]
            [mount.core :as mount]))

(defn app-fixture
  [f]
  (mount/start #'app/app)
  (f)
  (mount/stop #'app/app))

(defn db-fixture
  [f]
  (mount/start #'db/*db*)
  (f)
  (mount/stop #'db/*db*))

(use-fixtures :once app-fixture)
(use-fixtures :each db-fixture)

(deftest middleware
  (let [username  "hero@test.com"
        password  "awesome"
        form-data {:username username :password password}]
    (let [res (app/app (mock/request :post "/signup" form-data))]
      (is (= (:status res) 200))
      (is (-> res :body :token))
      (is (hashers/check password (get-in @db/*db* [:user/id username :password]))))
    (let [res (app/app (mock/request :post "/login" form-data))]
      (is (= (:status res) 200))
      (is (-> res :body :token)))))