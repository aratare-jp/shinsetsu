(ns shinsetsu.db.tag-test
  (:require [clojure.test :refer :all]
            [shinsetsu.db.user :refer :all]
            [shinsetsu.db.tag :refer :all]
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

(defn- tag-compare
  [expected actual]
  (expect (:tag/created actual))
  (expect (:tag/updated actual))
  (expect expected (in actual)))

(defexpect complete-test
  (testing "Normal path"
    (let [user    (create-user (g/generate User default-leaf-generator))
          user-id (:user/id user)]
      (doseq [tag (g/sample 50 Tag default-leaf-generator)]
        (let [tag        (merge tag
                                {:tag/user-id user-id})
              tag-id     (:tag/id tag)
              difference (-> (g/generate Tag default-leaf-generator)
                             (dissoc :tag/id)
                             (merge {:tag/user-id user-id}))
              new-tag    (merge tag difference)]
          (expect nil? (read-tag {:tag/id tag-id}))
          (tag-compare tag (create-tag tag))
          (tag-compare tag (read-tag {:tag/id tag-id}))
          (let [tags (read-user-tag {:user/id user-id})]
            (expect 1 (count tags))
            (expect tag (in (first tags))))
          (tag-compare new-tag (update-tag new-tag))
          (tag-compare new-tag (read-tag {:tag/id tag-id}))
          (tag-compare new-tag (delete-tag {:tag/id tag-id}))
          (expect nil? (read-tag {:tag/id tag-id})))))))

(comment
  (mount.core/start #'shinsetsu.config/env #'shinsetsu.db.core/db #'shinsetsu.db.core/migratus-config)
  (db/reset-db)
  (require '[eftest.runner :as ef])
  (ef/run-tests [#'shinsetsu.db.tag-test/complete-test]))