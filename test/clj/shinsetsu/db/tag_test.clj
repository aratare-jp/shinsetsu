(ns shinsetsu.db.tag-test
  (:require [clojure.test :refer :all]
            [shinsetsu.db.user :refer :all]
            [shinsetsu.db.tag :refer :all]
            [shinsetsu.config :refer [env]]
            [expectations.clojure.test :refer [defexpect expect more in]]
            [schema-generators.generators :as g]
            [shinsetsu.schemas :refer :all]
            [shinsetsu.db.utility :refer :all]
            [clojure.data :refer [diff]]))

(def db-fixture (get-db-fixture "shinsetsu-tag-db"))
(def db (:db db-fixture))
(use-fixtures :once (get-in db-fixture [:fixture :once]))
(use-fixtures :each (get-in db-fixture [:fixture :each]))

(defn- tag-compare
  [expected actual]
  (expect (:tag/created actual))
  (expect (:tag/updated actual))
  (expect expected (in actual)))

(defexpect complete-test
  (testing "Normal path"
    (let [user    (create-user db (g/generate User default-leaf-generator))
          user-id (:user/id user)]
      (doseq [tag (g/sample 50 Tag default-leaf-generator)]
        (let [tag        (merge tag
                                {:tag/user-id user-id})
              tag-id     (:tag/id tag)
              difference (-> (g/generate Tag default-leaf-generator)
                             (dissoc :tag/id)
                             (merge {:tag/user-id user-id}))
              new-tag    (merge tag difference)]
          (expect nil? (read-tag db {:tag/id tag-id}))
          (tag-compare tag (create-tag db tag))
          (tag-compare tag (read-tag db {:tag/id tag-id}))
          (let [tags (read-user-tag db {:user/id user-id})]
            (expect 1 (count tags))
            (expect tag (in (first tags))))
          (tag-compare new-tag (update-tag db new-tag))
          (tag-compare new-tag (read-tag db {:tag/id tag-id}))
          (tag-compare new-tag (delete-tag db {:tag/id tag-id}))
          (expect nil? (read-tag db {:tag/id tag-id})))))))
