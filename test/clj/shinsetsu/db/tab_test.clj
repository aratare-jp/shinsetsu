(ns shinsetsu.db.tab-test
  (:require
    [clojure.test :refer :all]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.db.tab :as tab-db]
    [taoensso.timbre :as log])
  (:import [org.postgresql.util PSQLException]))

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

(defexpect fail-create-tab-with-no-name (expect PSQLException (tab-db/create-tab {})))
(defexpect fail-create-tab-with-no-owner PSQLException (tab-db/create-tab {:tab/name "foo"}))

(defexpect normal-fetch-tabs
  (let [tab1-name    "foo"
        tab2-name    "bar"
        tab1         (tab-db/create-tab {:tab/name tab1-name :tab/user-id @user-id})
        tab2         (tab-db/create-tab {:tab/name tab2-name :tab/user-id @user-id})
        fetched-tabs (tab-db/fetch-tabs {:user/id @user-id})]
    (expect [tab1 tab2] fetched-tabs)))

(defexpect normal-fetch-empty-tabs [] (tab-db/fetch-tabs {:user/id @user-id}))
(defexpect normal-fetch-tabs-from-nonexistent-user [] (tab-db/fetch-tabs {}))

(defexpect normal-fetch-tab
  (let [tab-name     "hello"
        tab-password "world"
        tab          (tab-db/create-tab {:tab/name tab-name :tab/password tab-password :tab/user-id @user-id})
        tab-id       (:tab/id tab)
        fetched-tab  (tab-db/fetch-tab {:tab/id tab-id :user/id @user-id})]
    (expect tab fetched-tab)))

(defexpect fetch-nonexistent-tab nil? (tab-db/fetch-tab {}))

(comment
  (require '[kaocha.repl :as k])
  (k/run #'shinsetsu.db.tab-test/fetch-nonexistent-tab)
  (k/run 'shinsetsu.db.tab-test))
