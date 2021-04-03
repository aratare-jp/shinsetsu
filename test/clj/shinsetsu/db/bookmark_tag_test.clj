(ns shinsetsu.db.bookmark-tag-test
  (:require [clojure.test :refer :all]
            [shinsetsu.db.user :refer :all]
            [shinsetsu.db.tab :refer :all]
            [shinsetsu.db.tag :refer :all]
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
            [clojure.data :refer [diff]]))

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

(defexpect complete-test
  (testing "Normal path"
    (let [user    (create-user (g/generate User default-leaf-generator))
          user-id (:user/id user)
          tab     (create-tab (merge (g/generate Tab default-leaf-generator)
                                     {:tab/user-id user-id}))
          tab-id  (:tab/id tab)]
      (doseq [bookmark (g/sample 10 Bookmark default-leaf-generator)
              tag      (g/sample 10 Tag default-leaf-generator)]
        (let [bookmark     (merge bookmark
                                  {:bookmark/user-id user-id
                                   :bookmark/tab-id  tab-id})
              bookmark-id  (:bookmark/id bookmark)
              tag          (merge tag
                                  {:tag/user-id user-id})
              tag-id       (:tag/id tag)
              bookmark-tag {:bookmark-tag/bookmark-id bookmark-id
                            :bookmark-tag/tag-id      tag-id}]
          (if (nil? (read-bookmark {:bookmark/id bookmark-id}))
            (create-bookmark bookmark))
          (create-tag tag)
          (expect empty? (read-bookmark-tag {:bookmark/id bookmark-id}))
          (expect bookmark-tag (in (create-bookmark-tag bookmark-tag)))
          (let [actual (read-bookmark-tag {:bookmark/id bookmark-id})]
            (expect 1 (count actual))
            (expect bookmark-tag (in (first actual))))
          (expect bookmark-tag (in (delete-bookmark-tag bookmark-tag))))))))

(comment
  (mount.core/start)
  (require '[eftest.runner :as efr])
  (efr/run-tests [#'shinsetsu.db.bookmark-tag-test/complete-test]))