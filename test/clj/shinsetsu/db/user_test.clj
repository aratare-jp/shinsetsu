(ns shinsetsu.db.user-test
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [shinsetsu.db.user :as user-db])
  (:import [org.postgresql.util PSQLException]))

(use-fixtures :once db-setup)
(use-fixtures :each db-cleanup)

(defexpect normal-create-user
  (let [username "foo"
        password "bar"
        user     (user-db/create-user {:user/username username :user/password password})]
    (expect username (:user/username user))
    (expect password (:user/password user))
    (expect uuid? (:user/id user))
    (expect (complement nil?) (:user/created user))
    (expect (complement nil?) (:user/updated user))))

(defexpect create-null-user PSQLException (user-db/create-user {}))
(defexpect create-invalid-user PSQLException (user-db/create-user {:user/id nil :user/password nil}))

(defexpect create-duplicated-user
  (let [username "foo"
        password "bar"]
    (user-db/create-user {:user/username username :user/password password})
    (expect PSQLException (user-db/create-user {:user/username username :user/password password}))))

(defexpect normal-fetch-user-by-username
  (let [username "foo"
        password "bar"
        user     (user-db/create-user {:user/username username :user/password password})]
    (let [fetched-user (user-db/fetch-user-by-username {:user/username username})]
      (expect user fetched-user))))

(defexpect fetch-nonexistent-user-by-username nil? (user-db/fetch-user-by-username {:user/username "foo"}))
(defexpect fetch-empty-user-by-username nil? (user-db/fetch-user-by-username {}))
(defexpect fetch-null-user-by-username nil? (user-db/fetch-user-by-username {:user/username nil}))

(comment
  (user/start)
  (doc kaocha.repl/run)
  (require '[kaocha.repl :as k])
  (k/run 'shinsetsu.db.user-test)
  )
