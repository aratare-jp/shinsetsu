(ns shinsetsu.db.tab-test
  (:require
    [clojure.test :refer :all]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.db.tab :as tab-db]
    [taoensso.timbre :as log])
  (:import [java.util UUID]
           [clojure.lang ExceptionInfo]))

(def user (atom nil))
(def user-id (atom nil))

(defn user-setup
  [f]
  (reset! user (user-db/create-user {:user/username "foo" :user/password "bar"}))
  (reset! user-id (:user/id @user))
  (f))

(use-fixtures :once db-setup)
(use-fixtures :each db-cleanup user-setup)

(defexpect normal-create-tab
  (let [tab-name     "hello"
        tab-password "world"
        tab          (tab-db/create-tab {:tab/name tab-name :tab/password tab-password :tab/user-id @user-id})]
    (expect (complement nil?) (:tab/id tab))
    (expect tab-name (:tab/name tab))
    (expect tab-password (:tab/password tab))
    (expect (complement nil?) (:tab/created tab))
    (expect (complement nil?) (:tab/updated tab))
    (expect @user-id (:tab/user-id tab))))

(defexpect normal-create-tab-without-password
  (let [tab-name "hello"
        tab      (tab-db/create-tab {:tab/name tab-name :tab/user-id @user-id})]
    (expect (complement nil?) (:tab/id tab))
    (expect tab-name (:tab/name tab))
    (expect (complement nil?) (:tab/created tab))
    (expect (complement nil?) (:tab/updated tab))
    (expect @user-id (:tab/user-id tab))))

(defexpect fail-create-tab-with-no-name
  (try
    (tab-db/create-tab {:tab/user-id (UUID/randomUUID)})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid tab" message)
        (expect {:error-type :invalid-input :details {:tab/name ["missing required key"]}} data)))))

(defexpect fail-create-tab-with-empty-name
  (try
    (tab-db/create-tab {:tab/name "" :tab/user-id (UUID/randomUUID)})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid tab" message)
        (expect {:error-type :invalid-input :details {:tab/name ["should be at least 1 characters"]}} data)))))

(defexpect fail-create-tab-with-no-owner
  (try
    (tab-db/create-tab {:tab/name "foo"})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid tab" message)
        (expect {:error-type :invalid-input :details {:tab/user-id ["missing required key"]}} data)))))

(defexpect normal-fetch-tabs
  (let [tab1-name    "foo"
        tab2-name    "bar"
        tab1         (tab-db/create-tab {:tab/name tab1-name :tab/user-id @user-id})
        tab2         (tab-db/create-tab {:tab/name tab2-name :tab/user-id @user-id})
        fetched-tabs (tab-db/fetch-tabs {:user/id @user-id})]
    (expect [tab1 tab2] fetched-tabs)))

(defexpect normal-fetch-empty-tabs [] (tab-db/fetch-tabs {:user/id @user-id}))
(defexpect normal-fetch-tabs-from-nonexistent-user [] (tab-db/fetch-tabs {:user/id (UUID/randomUUID)}))

(defexpect fail-fetch-tabs-without-user-id
  (try
    (tab-db/fetch-tabs {})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid user ID" message)
        (expect {:error-type :invalid-input :details {:user/id ["missing required key"]}} data)))))

(defexpect fail-fetch-tabs-with-wrong-user-id
  (try
    (tab-db/fetch-tabs {:user/id "boo"})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid user ID" message)
        (expect {:error-type :invalid-input :details {:user/id ["should be a uuid"]}} data)))))

(defexpect normal-fetch-tab
  (let [tab-name     "hello"
        tab-password "world"
        tab          (tab-db/create-tab {:tab/name tab-name :tab/password tab-password :tab/user-id @user-id})
        tab-id       (:tab/id tab)
        fetched-tab  (tab-db/fetch-tab {:tab/id tab-id :user/id @user-id})]
    (expect tab fetched-tab)))

(defexpect normal-fetch-nonexistent-tab
  (let [fetched-tab (tab-db/fetch-tab {:tab/id (UUID/randomUUID) :user/id @user-id})]
    (expect nil fetched-tab)))

(defexpect fail-fetch-tab-with-no-tab-id
  (try
    (tab-db/fetch-tab {:user/id (UUID/randomUUID)})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid user or tab ID" message)
        (expect {:error-type :invalid-input :details {:tab/id ["missing required key"]}} data)))))

(defexpect fail-fetch-tab-with-wrong-tab-id
  (try
    (tab-db/fetch-tab {:tab/id "not real" :user/id (UUID/randomUUID)})
    (expect false)
    (catch ExceptionInfo e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid user or tab ID" message)
        (expect {:error-type :invalid-input :details {:tab/id ["should be a uuid"]}} data)))))

(defexpect fail-fetch-tab-with-no-user-id
  (try
    (tab-db/fetch-tab {:tab/id (UUID/randomUUID)})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid user or tab ID" message)
        (expect {:error-type :invalid-input :details {:user/id ["missing required key"]}} data)))))

(defexpect fail-fetch-tab-with-wrong-user-id
  (try
    (tab-db/fetch-tab {:user/id "not real" :tab/id (UUID/randomUUID)})
    (expect false)
    (catch ExceptionInfo e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid user or tab ID" message)
        (expect {:error-type :invalid-input :details {:user/id ["should be a uuid"]}} data)))))

(comment
  (require '[kaocha.repl :as k])
  (k/run #'shinsetsu.db.tab-test/normal-fetch-tab)
  (k/run 'shinsetsu.db.tab-test))
