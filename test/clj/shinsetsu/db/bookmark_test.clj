(ns shinsetsu.db.bookmark-test
  (:require [clojure.test :refer :all]
            [shinsetsu.db.user :refer :all]
            [shinsetsu.db.tab :refer :all]
            [shinsetsu.db.bookmark :refer :all]
            [shinsetsu.config :refer [env]]
            [expectations.clojure.test :refer [defexpect expect more in]]
            [schema-generators.generators :as g]
            [shinsetsu.schemas :refer :all]
            [shinsetsu.db.utility :refer :all]
            [clojure.data :refer [diff]])
  (:import [java.util Arrays]))

(def db-fixture (get-db-fixture "shinsetsu-bookmark-db"))
(def db (:db db-fixture))
(use-fixtures :once (get-in db-fixture [:fixture :once]))
(use-fixtures :each (get-in db-fixture [:fixture :each]))

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
    (let [user    (create-user db (g/generate User default-leaf-generator))
          user-id (:user/id user)
          tab     (create-tab db (merge (g/generate Tab default-leaf-generator)
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
          (expect nil? (read-bookmark db {:bookmark/id bookmark-id}))
          (bookmark-compare bookmark (create-bookmark db bookmark))
          (bookmark-compare bookmark (read-bookmark db {:bookmark/id bookmark-id}))
          (bookmark-compare new-bookmark (update-bookmark db new-bookmark))
          (bookmark-compare new-bookmark (read-bookmark db {:bookmark/id bookmark-id}))
          (bookmark-compare new-bookmark (delete-bookmark db {:bookmark/id bookmark-id}))
          (expect nil? (read-bookmark db {:bookmark/id bookmark-id})))))))
