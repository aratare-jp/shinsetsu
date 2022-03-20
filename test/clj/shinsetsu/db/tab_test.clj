(ns shinsetsu.db.tab-test
  (:require
    [clojure.test :refer :all]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.db.tab :as tab-db]
    [shinsetsu.db.bookmark :as bookmark-db]
    [taoensso.timbre :as log]
    [malli.error :as me]
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

(defexpect fail-create-tab-without-name
  (try
    (tab-db/create-tab {:tab/user-id (UUID/randomUUID)})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid tab" message)
        (expect {:error-type :invalid-input :error-data {:tab/name ["missing required key"]}} data)))))

(defexpect fail-create-tab-with-invalid-name
  (try
    (tab-db/create-tab {:tab/name "" :tab/user-id (UUID/randomUUID)})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid tab" message)
        (expect {:error-type :invalid-input :error-data {:tab/name ["should be at least 1 characters"]}} data)))))

(defexpect fail-create-tab-without-user
  (try
    (tab-db/create-tab {:tab/name "foo"})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid tab" message)
        (expect {:error-type :invalid-input :error-data {:tab/user-id ["missing required key"]}} data)))))

(defexpect fail-create-tab-with-invalid-user
  (try
    (tab-db/create-tab {:tab/user-id "foo" :tab/name "foo"})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid tab" message)
        (expect {:error-type :invalid-input :error-data {:tab/user-id ["should be a uuid"]}} data)))))

(defexpect normal-patch-tab-with-new-name-and-password
  (let [tab              (tab-db/create-tab {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id})
        new-tab-name     "hello"
        new-tab-password "world"
        tab-id           (:tab/id tab)
        patched-tab      (tab-db/patch-tab {:tab/id       tab-id
                                            :tab/name     new-tab-name
                                            :tab/password new-tab-password
                                            :tab/user-id  @user-id})]
    (expect (:tab/id tab) (:tab/id patched-tab))
    (expect new-tab-name (:tab/name patched-tab))
    (expect new-tab-password (:tab/password patched-tab))
    (expect (:tab/created tab) (:tab/created patched-tab))
    (expect #(.after % (:tab/updated tab)) (:tab/updated patched-tab))
    (expect @user-id (:tab/user-id patched-tab))))

(defexpect normal-patch-tab-with-new-name
  (let [tab          (tab-db/create-tab {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id})
        new-tab-name "hello"
        tab-id       (:tab/id tab)
        patched-tab  (tab-db/patch-tab {:tab/id      tab-id
                                        :tab/name    new-tab-name
                                        :tab/user-id @user-id})]
    (expect (:tab/id tab) (:tab/id patched-tab))
    (expect new-tab-name (:tab/name patched-tab))
    (expect (:tab/password tab) (:tab/password patched-tab))
    (expect (:tab/created tab) (:tab/created patched-tab))
    (expect #(.after % (:tab/updated tab)) (:tab/updated patched-tab))
    (expect @user-id (:tab/user-id patched-tab))))

(defexpect normal-patch-tab-with-new-password
  (let [tab              (tab-db/create-tab {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id})
        new-tab-password "hello"
        tab-id           (:tab/id tab)
        patched-tab      (tab-db/patch-tab {:tab/id       tab-id
                                            :tab/password new-tab-password
                                            :tab/user-id  @user-id})]
    (expect (:tab/id tab) (:tab/id patched-tab))
    (expect (:tab/name tab) (:tab/name patched-tab))
    (expect new-tab-password (:tab/password patched-tab))
    (expect (:tab/created tab) (:tab/created patched-tab))
    (expect #(.after % (:tab/updated tab)) (:tab/updated patched-tab))
    (expect @user-id (:tab/user-id patched-tab))))

(defexpect normal-patch-tab-without-new-name-and-password
  (let [tab         (tab-db/create-tab {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id})
        tab-id      (:tab/id tab)
        patched-tab (tab-db/patch-tab {:tab/id tab-id :tab/user-id @user-id})]
    (expect (:tab/id tab) (:tab/id patched-tab))
    (expect (:tab/name tab) (:tab/name patched-tab))
    (expect (:tab/password tab) (:tab/password patched-tab))
    (expect (:tab/created tab) (:tab/created patched-tab))
    (expect #(.after % (:tab/updated tab)) (:tab/updated patched-tab))
    (expect @user-id (:tab/user-id patched-tab))))

(defexpect fail-patch-tab-with-invalid-name
  (try
    (let [tab    (tab-db/create-tab {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id})
          tab-id (:tab/id tab)]
      (tab-db/patch-tab {:tab/id tab-id :tab/user-id @user-id :tab/name ""})
      (expect false))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid tab" message)
        (expect {:error-type :invalid-input :error-data {:tab/name ["should be at least 1 characters"]}} data)))))

(defexpect fail-patch-tab-with-invalid-password
  (try
    (let [tab    (tab-db/create-tab {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id})
          tab-id (:tab/id tab)]
      (tab-db/patch-tab {:tab/id tab-id :tab/user-id @user-id :tab/password ""})
      (expect false))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid tab" message)
        (expect {:error-type :invalid-input :error-data {:tab/password ["should be at least 1 characters"]}} data)))))

(defexpect normal-delete-tab-with-no-bookmarks
  (let [tab         (tab-db/create-tab {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id})
        tab-id      (:tab/id tab)
        deleted-tab (tab-db/delete-tab {:tab/id tab-id :tab/user-id @user-id})]
    (expect tab deleted-tab)
    (expect nil (tab-db/fetch-tab {:tab/id tab-id :user/id @user-id}))))

(defexpect normal-delete-tab-with-bookmarks
  (let [tab         (tab-db/create-tab {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id})
        tab-id      (:tab/id tab)
        bookmark1   (bookmark-db/create-bookmark {:bookmark/title "foo" :bookmark/url "blah" :bookmark/tab-id tab-id :bookmark/user-id @user-id})
        bookmark2   (bookmark-db/create-bookmark {:bookmark/title "fim" :bookmark/url "bloo" :bookmark/tab-id tab-id :bookmark/user-id @user-id})
        deleted-tab (tab-db/delete-tab {:tab/id tab-id :tab/user-id @user-id})]
    (expect tab deleted-tab)
    (expect nil (tab-db/fetch-tab {:tab/id tab-id :user/id @user-id}))
    (expect [] (bookmark-db/fetch-bookmarks {:tab/id tab-id :user/id @user-id}))))

(defexpect normal-delete-tab-with-nonexistent-id
  (let [tab-id      (UUID/randomUUID)
        deleted-tab (tab-db/delete-tab {:tab/id tab-id :tab/user-id @user-id})]
    (expect nil deleted-tab)))

(defexpect normal-delete-tab-with-nonexistent-user-id
  (let [tab         (tab-db/create-tab {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id})
        tab-id      (:tab/id tab)
        user-id     (UUID/randomUUID)
        deleted-tab (tab-db/delete-tab {:tab/id tab-id :tab/user-id user-id})]
    (expect nil deleted-tab)))

(defexpect fail-delete-tab-without-tab-id
  (try
    (tab-db/delete-tab {:tab/user-id @user-id})
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid tab" message)
        (expect {:error-type :invalid-input :error-data {:tab/id ["missing required key"]}} data)))))

(defexpect fail-delete-tab-with-invalid-tab-id
  (try
    (tab-db/delete-tab {:tab/id "" :tab/user-id @user-id})
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid tab" message)
        (expect {:error-type :invalid-input :error-data {:tab/id ["should be a uuid"]}} data)))))

(defexpect fail-delete-tab-without-user-id
  (try
    (let [tab    (tab-db/create-tab {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id})
          tab-id (:tab/id tab)]
      (tab-db/delete-tab {:tab/id tab-id}))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid tab" message)
        (expect {:error-type :invalid-input :error-data {:tab/user-id ["missing required key"]}} data)))))

(defexpect fail-delete-tab-with-invalid-user-id
  (try
    (let [tab    (tab-db/create-tab {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id})
          tab-id (:tab/id tab)]
      (tab-db/delete-tab {:tab/id tab-id :tab/user-id ""}))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid tab" message)
        (expect {:error-type :invalid-input :error-data {:tab/user-id ["should be a uuid"]}} data)))))

(defexpect normal-fetch-tabs
  (let [tab1-name    "foo"
        tab2-name    "bar"
        tab1         (tab-db/create-tab {:tab/name tab1-name :tab/user-id @user-id})
        tab2         (tab-db/create-tab {:tab/name tab2-name :tab/user-id @user-id})
        fetched-tabs (tab-db/fetch-tabs {:user/id @user-id})]
    (expect [tab1 tab2] fetched-tabs)))

(defexpect normal-fetch-empty-tabs [] (tab-db/fetch-tabs {:user/id @user-id}))
(defexpect normal-fetch-tabs-from-nonexistent-user [] (tab-db/fetch-tabs {:user/id (UUID/randomUUID)}))

(defexpect fail-fetch-tabs-without-user
  (try
    (tab-db/fetch-tabs {})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid user ID" message)
        (expect {:error-type :invalid-input :error-data {:user/id ["missing required key"]}} data)))))

(defexpect fail-fetch-tabs-with-invalid-user
  (try
    (tab-db/fetch-tabs {:user/id "boo"})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid user ID" message)
        (expect {:error-type :invalid-input :error-data {:user/id ["should be a uuid"]}} data)))))

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

(defexpect fail-fetch-tab-without-id-and-tab-id
  (try
    (tab-db/fetch-tab {})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid user or tab ID" message)
        (expect
          {:error-type :invalid-input
           :error-data {:tab/id  ["missing required key"]
                        :user/id ["missing required key"]}}
          data)))))

(defexpect fail-fetch-tab-without-id
  (try
    (tab-db/fetch-tab {:user/id (UUID/randomUUID)})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid user or tab ID" message)
        (expect {:error-type :invalid-input :error-data {:tab/id ["missing required key"]}} data)))))

(defexpect fail-fetch-tab-with-invalid-id
  (try
    (tab-db/fetch-tab {:tab/id "foo" :user/id (UUID/randomUUID)})
    (expect false)
    (catch ExceptionInfo e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid user or tab ID" message)
        (expect {:error-type :invalid-input :error-data {:tab/id ["should be a uuid"]}} data)))))

(defexpect fail-fetch-tab-without-user
  (try
    (tab-db/fetch-tab {:tab/id (UUID/randomUUID)})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid user or tab ID" message)
        (expect {:error-type :invalid-input :error-data {:user/id ["missing required key"]}} data)))))

(defexpect fail-fetch-tab-with-invalid-user
  (try
    (tab-db/fetch-tab {:user/id "not real" :tab/id (UUID/randomUUID)})
    (expect false)
    (catch ExceptionInfo e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid user or tab ID" message)
        (expect {:error-type :invalid-input :error-data {:user/id ["should be a uuid"]}} data)))))

(comment
  (require '[kaocha.repl :as k])
  (k/run #'shinsetsu.db.tab-test/normal-fetch-tab)
  (k/run 'shinsetsu.db.tab-test))
