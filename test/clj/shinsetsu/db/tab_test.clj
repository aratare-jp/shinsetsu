(ns shinsetsu.db.tab-test
  (:require
    [clojure.test :refer :all]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.db.tab :as tab-db]
    [shinsetsu.db.bookmark :as bookmark-db]
    [taoensso.timbre :as log]
    [shinsetsu.db.bookmark :as bookmark-db])
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

;; CREATE

(defexpect normal-create-tab-with-password
  (let [tab-name     "hello"
        tab-password "world"
        tab          (tab-db/create-tab #:tab{:name tab-name :password tab-password :user-id @user-id})]
    (expect uuid? (:tab/id tab))
    (expect tab-name (:tab/name tab))
    (expect tab-password (:tab/password tab))
    (expect inst? (:tab/created tab))
    (expect inst? (:tab/updated tab))
    (expect @user-id (:tab/user-id tab))))

(defexpect normal-create-tab-without-password
  (let [tab-name "hello"
        tab      (tab-db/create-tab {:tab/name tab-name :tab/user-id @user-id})]
    (expect uuid? (:tab/id tab))
    (expect tab-name (:tab/name tab))
    (expect nil (:tab/password tab))
    (expect inst? (:tab/created tab))
    (expect inst? (:tab/updated tab))
    (expect @user-id (:tab/user-id tab))))

(defexpect fail-to-create-tab-with-null-password
  (try
    (tab-db/create-tab {:tab/name "foo" :tab/password nil :tab/user-id @user-id})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tab/password ["should be a string"]}} data)))))

(defexpect fail-to-create-tab-with-empty-password
  (try
    (tab-db/create-tab {:tab/name "foo" :tab/password "" :tab/user-id @user-id})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tab/password ["should be at least 1 characters"]}} data)))))

(defexpect fail-to-create-tab-without-name
  (try
    (tab-db/create-tab {:tab/user-id @user-id})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tab/name ["missing required key"]}} data)))))

(defexpect fail-to-create-tab-with-empty-name
  (try
    (tab-db/create-tab {:tab/name "" :tab/user-id @user-id})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tab/name ["should be at least 1 characters"]}} data)))))

(defexpect fail-to-create-tab-with-null-name
  (try
    (tab-db/create-tab {:tab/name nil :tab/user-id @user-id})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tab/name ["should be a string"]}} data)))))

;; PATCH

(defexpect normal-patch-tab-with-new-name-and-password
  (let [tab         (tab-db/create-tab #:tab{:name "foo" :password "bar" :user-id @user-id})
        tab-id      (:tab/id tab)
        new-name    "hello"
        new-pwd     "world"
        patched-tab (tab-db/patch-tab #:tab{:id tab-id :name new-name :password new-pwd :user-id @user-id})]
    (expect (:tab/id tab) (:tab/id patched-tab))
    (expect new-name (:tab/name patched-tab))
    (expect new-pwd (:tab/password patched-tab))
    (expect (:tab/created tab) (:tab/created patched-tab))
    (expect #(.after % (:tab/updated tab)) (:tab/updated patched-tab))
    (expect @user-id (:tab/user-id patched-tab))))

(defexpect normal-patch-tab-with-new-name
  (let [tab         (tab-db/create-tab #:tab{:name "foo" :password "bar" :user-id @user-id})
        tab-id      (:tab/id tab)
        new-name    "hello"
        patched-tab (tab-db/patch-tab #:tab{:id tab-id :name new-name :user-id @user-id})]
    (expect (:tab/id tab) (:tab/id patched-tab))
    (expect new-name (:tab/name patched-tab))
    (expect (:tab/password tab) (:tab/password patched-tab))
    (expect (:tab/created tab) (:tab/created patched-tab))
    (expect #(.after % (:tab/updated tab)) (:tab/updated patched-tab))
    (expect @user-id (:tab/user-id patched-tab))))

(defexpect normal-patch-tab-with-new-password
  (let [tab         (tab-db/create-tab #:tab{:name "foo" :password "bar" :user-id @user-id})
        tab-id      (:tab/id tab)
        new-pwd     "hello"
        patched-tab (tab-db/patch-tab #:tab{:id tab-id :password new-pwd :user-id @user-id})]
    (expect (:tab/id tab) (:tab/id patched-tab))
    (expect (:tab/name tab) (:tab/name patched-tab))
    (expect new-pwd (:tab/password patched-tab))
    (expect (:tab/created tab) (:tab/created patched-tab))
    (expect #(.after % (:tab/updated tab)) (:tab/updated patched-tab))
    (expect @user-id (:tab/user-id patched-tab))))

(defexpect normal-patch-tab-without-new-name-and-password
  (let [tab         (tab-db/create-tab #:tab{:name "foo" :password "bar" :user-id @user-id})
        tab-id      (:tab/id tab)
        patched-tab (tab-db/patch-tab {:tab/id tab-id :tab/user-id @user-id})]
    (expect (:tab/id tab) (:tab/id patched-tab))
    (expect (:tab/name tab) (:tab/name patched-tab))
    (expect (:tab/password tab) (:tab/password patched-tab))
    (expect (:tab/created tab) (:tab/created patched-tab))
    (expect #(.after % (:tab/updated tab)) (:tab/updated patched-tab))
    (expect @user-id (:tab/user-id patched-tab))))

(defexpect fail-to-patch-tab-with-invalid-name
  (try
    (let [tab    (tab-db/create-tab #:tab{:name "foo" :password "bar" :user-id @user-id})
          tab-id (:tab/id tab)]
      (tab-db/patch-tab #:tab{:id tab-id :user-id @user-id :name ""})
      (expect false))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tab/name ["should be at least 1 characters"]}} data)))))

(defexpect fail-to-patch-tab-with-invalid-password
  (try
    (let [tab    (tab-db/create-tab #:tab{:name "foo" :password "bar" :user-id @user-id})
          tab-id (:tab/id tab)]
      (tab-db/patch-tab #:tab{:id tab-id :user-id @user-id :password ""})
      (expect false))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tab/password ["should be at least 1 characters"]}} data)))))

(defexpect fail-to-patch-tab-with-null-name
  (try
    (let [tab    (tab-db/create-tab #:tab{:name "foo" :password "bar" :user-id @user-id})
          tab-id (:tab/id tab)]
      (tab-db/patch-tab #:tab{:id tab-id :user-id @user-id :name nil})
      (expect false))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tab/name ["should be a string"]}} data)))))

(defexpect fail-to-patch-tab-with-null-password
  (try
    (let [tab    (tab-db/create-tab #:tab{:name "foo" :password "bar" :user-id @user-id})
          tab-id (:tab/id tab)]
      (tab-db/patch-tab #:tab{:id tab-id :user-id @user-id :password nil})
      (expect false))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tab/password ["should be a string"]}} data)))))

;; DELETE

(defexpect normal-delete-tab-with-no-bookmarks
  (let [tab         (tab-db/create-tab #:tab{:name "foo" :password "bar" :user-id @user-id})
        tab-id      (:tab/id tab)
        deleted-tab (tab-db/delete-tab {:tab/id tab-id :tab/user-id @user-id})]
    (expect tab deleted-tab)
    (expect nil (tab-db/fetch-tab {:tab/id tab-id :tab/user-id @user-id}))))

(defexpect normal-delete-tab-with-bookmarks
  (let [tab         (tab-db/create-tab #:tab{:name "foo" :password "bar" :user-id @user-id})
        tab-id      (:tab/id tab)
        _           (bookmark-db/create-bookmark #:bookmark{:title "foo" :url "blah" :tab-id tab-id :user-id @user-id})
        _           (bookmark-db/create-bookmark #:bookmark{:title "fim" :url "bloo" :tab-id tab-id :user-id @user-id})
        deleted-tab (tab-db/delete-tab {:tab/id tab-id :tab/user-id @user-id})]
    (expect tab deleted-tab)
    (expect nil (tab-db/fetch-tab {:tab/id tab-id :tab/user-id @user-id}))
    (expect [] (bookmark-db/fetch-bookmarks {:bookmark/tab-id tab-id :bookmark/user-id @user-id}))))

(defexpect normal-delete-tab-with-nonexistent-id
  (let [deleted-tab (tab-db/delete-tab {:tab/id (UUID/randomUUID) :tab/user-id @user-id})]
    (expect nil deleted-tab)))

;; FETCH TABS

(defexpect normal-fetch-tabs
  (let [tab1-name    "foo"
        tab2-name    "bar"
        tab1         (tab-db/create-tab {:tab/name tab1-name :tab/user-id @user-id})
        tab2         (tab-db/create-tab {:tab/name tab2-name :tab/user-id @user-id})
        fetched-tabs (tab-db/fetch-tabs {:tab/user-id @user-id})]
    (expect [tab1 tab2] fetched-tabs)))

(defexpect normal-fetch-empty-tabs [] (tab-db/fetch-tabs {:tab/user-id @user-id}))
(defexpect normal-fetch-tabs-from-nonexistent-user [] (tab-db/fetch-tabs {:tab/user-id (UUID/randomUUID)}))

;; FETCH TAB

(defexpect normal-fetch-tab
  (let [tab         (tab-db/create-tab #:tab{:name "foo" :password "bar" :user-id @user-id})
        tab-id      (:tab/id tab)
        fetched-tab (tab-db/fetch-tab {:tab/id tab-id :tab/user-id @user-id})]
    (expect tab fetched-tab)))

(defexpect normal-fetch-nonexistent-tab
  (let [fetched-tab (tab-db/fetch-tab {:tab/id (UUID/randomUUID) :tab/user-id @user-id})]
    (expect nil fetched-tab)))

(defexpect fail-to-fetch-tab-with-invalid-id
  (try
    (tab-db/fetch-tab {:tab/id "foo" :tab/user-id (UUID/randomUUID)})
    (expect false)
    (catch ExceptionInfo e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tab/id ["should be a uuid"]}} data)))))

(defexpect fail-to-fetch-tab-with-invalid-user
  (try
    (tab-db/fetch-tab {:tab/user-id "not real" :tab/id (UUID/randomUUID)})
    (expect false)
    (catch ExceptionInfo e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tab/user-id ["should be a uuid"]}} data)))))

(comment
  (require '[kaocha.repl :as k])
  (k/run #'shinsetsu.db.tab-test/fail-create-tab-with-invalid-password)
  (k/run 'shinsetsu.db.tab-test))
