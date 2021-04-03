(ns shinsetsu.db.bookmark-tag-test
  (:require [clojure.test :refer :all]
            [shinsetsu.db.user :refer :all]
            [shinsetsu.db.tab :refer :all]
            [shinsetsu.db.tag :refer :all]
            [shinsetsu.db.bookmark :refer :all]
            [shinsetsu.db.bookmark-tag :refer :all]
            [shinsetsu.config :refer [env]]
            [expectations.clojure.test :refer [defexpect expect more in]]
            [schema-generators.generators :as g]
            [shinsetsu.schemas :refer :all]
            [shinsetsu.db.utility :refer :all]
            [clojure.data :refer [diff]]))

(def db-fixture (get-db-fixture "shinsetsu-bookmark-tag-db"))
(def db (:db db-fixture))
(use-fixtures :once (get-in db-fixture [:fixture :once]))
(use-fixtures :each (get-in db-fixture [:fixture :each]))

(defexpect complete-test
  (testing "Normal path"
    (let [user    (create-user db (g/generate User default-leaf-generator))
          user-id (:user/id user)
          tab     (create-tab db (merge (g/generate Tab default-leaf-generator)
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
          (if (nil? (read-bookmark db {:bookmark/id bookmark-id}))
            (create-bookmark db bookmark))
          (create-tag db tag)
          (expect empty? (read-bookmark-tag db {:bookmark/id bookmark-id}))
          (expect bookmark-tag (in (create-bookmark-tag db bookmark-tag)))
          (let [actual (read-bookmark-tag db {:bookmark/id bookmark-id})]
            (expect 1 (count actual))
            (expect bookmark-tag (in (first actual))))
          (expect bookmark-tag (in (delete-bookmark-tag db bookmark-tag))))))))
