(ns shinsetsu.db.user-test
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.db.test-utility :refer [db-setup]]
    [shinsetsu.db.user :as user-db]
    [taoensso.timbre :as log]))

(use-fixtures :each db-setup)

(defexpect normal-create-user
  (let [username "foo"
        password "bar"
        user     (user-db/create-user {:user/username username :user/password password})]
    (expect username (:user/username user))
    (expect password (:user/password user))
    (expect uuid? (:user/id user))
    (expect (complement nil?) (:user/created user))
    (expect (complement nil?) (:user/updated user))
    (let [fetched-user (user-db/fetch-user-by-username {:user/username username})]
      (expect user fetched-user))))
