(ns shinsetsu.db.bookmark-test
  (:require [clojure.test :refer :all]
            [shinsetsu.db.user :refer :all]
            [shinsetsu.db.tab :refer :all]
            [shinsetsu.db.bookmark :refer :all]
            [shinsetsu.db.bookmark-tag :refer :all]
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

(defn once-fixture
  [f]
  (mount/start #'env #'db/db #'db/migratus-config)
  (db/migrate)
  (f)
  (mount/stop #'env #'db/db #'db/migratus-config))

(defn each-fixture
  [f]
  (f)
  (log/info "Resetting db")
  (db/reset-db))

(use-fixtures :once once-fixture)
(use-fixtures :each each-fixture)

(defn- bookmark-compare
  [expected actual]
  (expect (:bookmark/created actual))
  (expect (:bookmark/updated actual))
  (let [image          (bytes (:bookmark/image actual))
        expected-image (bytes (:bookmark/image expected))
        actual         (dissoc actual :bookmark/image)
        expected       (dissoc expected :bookmark/image)]
    (expect expected (in actual))
    (expect (Arrays/equals image expected-image))))

(defexpect complete-test
  (testing "Normal path"
    (let [user    (create-user (g/generate User default-leaf-generator))
          user-id (:user/id user)
          tab     (create-tab (merge (g/generate Tab default-leaf-generator)
                                     {:tab/user-id user-id}))
          tab-id  (:tab/id tab)]
      (doseq [bookmark (g/sample 50 Bookmark default-leaf-generator)]
        (let [bookmark     (merge bookmark
                                  {:bookmark/user-id user-id
                                   :bookmark/tab-id  tab-id})
              bookmark-id  (:bookmark/id bookmark)
              difference   (-> (g/generate Bookmark default-leaf-generator)
                               (dissoc :bookmark/id)
                               (merge {:bookmark/user-id user-id
                                       :bookmark/tab-id  tab-id}))
              new-bookmark (merge bookmark difference)]
          (expect nil? (read-bookmark {:bookmark/id bookmark-id}))
          (bookmark-compare bookmark (create-bookmark bookmark))
          (bookmark-compare bookmark (read-bookmark {:bookmark/id bookmark-id}))
          (bookmark-compare new-bookmark (update-bookmark new-bookmark))
          (bookmark-compare new-bookmark (read-bookmark {:bookmark/id bookmark-id}))
          (bookmark-compare new-bookmark (delete-bookmark {:bookmark/id bookmark-id}))
          (expect nil? (read-bookmark {:bookmark/id bookmark-id})))))))

(comment
  (mount.core/start #'shinsetsu.config/env #'shinsetsu.db.core/db #'shinsetsu.db.core/migratus-config)
  (db/reset-db)
  (require '[eftest.runner :as ef])
  (ef/run-tests [#'shinsetsu.db.bookmark-test/complete-test]))
