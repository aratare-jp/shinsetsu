(ns shinsetsu.db.user-test
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [shinsetsu.db.user :as user-db])
  (:import [org.postgresql.util PSQLException]
           [java.util UUID]))

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

(defexpect fail-create-user-without-username-and-password
  (try
    (user-db/create-user {})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid user" message)
        (expect
          {:error-type :invalid-input
           :error-data {:user/username ["missing required key"]
                        :user/password ["missing required key"]}}
          data)))))

(defexpect fail-create-user-without-username
  (try
    (user-db/create-user {:user/password "foo"})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid user" message)
        (expect
          {:error-type :invalid-input
           :error-data {:user/username ["missing required key"]}}
          data)))))

(defexpect fail-create-user-with-invalid-username
  (try
    (user-db/create-user {:user/username "" :user/password "foo"})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid user" message)
        (expect
          {:error-type :invalid-input
           :error-data {:user/username ["should be at least 1 characters"]}}
          data)))))

(defexpect fail-create-user-without-password
  (try
    (user-db/create-user {:user/username "foo"})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid user" message)
        (expect
          {:error-type :invalid-input
           :error-data {:user/password ["missing required key"]}}
          data)))))

(defexpect fail-create-user-with-invalid-password
  (try
    (user-db/create-user {:user/username "foo" :user/password ""})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid user" message)
        (expect
          {:error-type :invalid-input
           :error-data {:user/password ["should be at least 1 characters"]}}
          data)))))

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

(defexpect fail-fetch-user-without-username
  (try
    (user-db/fetch-user-by-username {})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid username" message)
        (expect {:error-type :invalid-input :error-data {:user/username ["missing required key"]}} data)))))

(defexpect fail-fetch-user-with-invalid-username
  (try
    (user-db/fetch-user-by-username {:user/username ""})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid username" message)
        (expect {:error-type :invalid-input :error-data {:user/username ["should be at least 1 characters"]}} data)))))

(defexpect normal-fetch-user-by-id
  (let [username "foo"
        password "bar"
        user     (user-db/create-user {:user/username username :user/password password})]
    (let [fetched-user (user-db/fetch-user-by-id {:user/id (:user/id user)})]
      (expect user fetched-user))))

(defexpect fetch-nonexistent-user-by-id nil? (user-db/fetch-user-by-id {:user/id (UUID/randomUUID)}))

(defexpect fail-fetch-user-without-id
  (try
    (user-db/fetch-user-by-id {})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid user ID" message)
        (expect {:error-type :invalid-input :error-data {:user/id ["missing required key"]}} data)))))

(defexpect fail-fetch-user-with-invalid-id
  (try
    (user-db/fetch-user-by-id {:user/id ""})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid user ID" message)
        (expect {:error-type :invalid-input :error-data {:user/id ["should be a uuid"]}} data)))))

(comment
  (user/start)
  (doc kaocha.repl/run)
  (require '[kaocha.repl :as k])
  (k/run 'shinsetsu.db.user-test)
  )
