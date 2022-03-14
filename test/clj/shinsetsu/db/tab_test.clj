(ns shinsetsu.db.tab-test
  (:require
    [clojure.test :refer :all]
    [shinsetsu.db.test-utility :refer [db-setup db-cleanup]]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.db.tab :as tab-db]
    [taoensso.timbre :as log]))

(def user (atom nil))

(defn user-setup
  [f]
  (reset! user (user-db/create-user {:user/username "foo" :user/password "bar"}))
  (f)
  (reset! user nil))

(use-fixtures :once db-setup)
(use-fixtures :each db-cleanup user-setup)

(defexpect normal-create-tab-without-password
  (let [tab-name "hello"
        user-id  (:user/id @user)
        tab      (tab-db/create-tab {:tab/name tab-name :tab/user-id user-id})]
    (expect (complement nil?) (:tab/id tab))
    (expect tab-name (:tab/name tab))
    (expect (complement nil?) (:tab/created tab))
    (expect (complement nil?) (:tab/updated tab))
    (expect user-id (:tab/user-id tab))))

(comment
  (require '[kaocha.repl :as k])
  (k/run 'shinsetsu.db.tab-test))
