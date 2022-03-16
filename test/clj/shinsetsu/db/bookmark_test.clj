(ns shinsetsu.db.bookmark-test
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.db.tab :as tab-db]
    [shinsetsu.db.bookmark :as bookmark-db]
    [shinsetsu.schema :as s]
    [malli.core :as m]
    [malli.error :as em]
    [malli.error :as me])
  (:import [org.postgresql.util PSQLException]
           [java.util UUID]))

(def user (atom nil))
(def user-id (atom nil))
(def tab (atom nil))
(def tab-id (atom nil))

(defn user-tab-setup
  [f]
  (reset! user (user-db/create-user {:user/username "foo" :user/password "bar"}))
  (reset! user-id (:user/id @user))
  (reset! tab (tab-db/create-tab {:tab/name "baz" :tab/user-id @user-id}))
  (reset! tab-id (:tab/id @tab))
  (f))

(use-fixtures :once db-setup)
(use-fixtures :each db-cleanup user-tab-setup)

(defexpect normal-create-bookmark
  (let [bookmark-title "hello"
        bookmark-url   "world"
        bookmark       (bookmark-db/create-bookmark {:bookmark/title   bookmark-title
                                                     :bookmark/url     bookmark-url
                                                     :bookmark/tab-id  @tab-id
                                                     :bookmark/user-id @user-id})]
    (expect (complement nil?) (:bookmark/id bookmark))
    (expect bookmark-title (:bookmark/title bookmark))
    (expect bookmark-url (:bookmark/url bookmark))
    (expect (complement nil?) (:bookmark/created bookmark))
    (expect (complement nil?) (:bookmark/updated bookmark))
    (expect @tab-id (:bookmark/tab-id bookmark))
    (expect @user-id (:bookmark/user-id bookmark))))

(defexpect fail-create-bookmark-without-title
  (try
    (bookmark-db/create-bookmark {:bookmark/url     "foo"
                                  :bookmark/tab-id  @tab-id
                                  :bookmark/user-id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid bookmark" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/title ["missing required key"]}} data)))))

(defexpect fail-create-bookmark-with-invalid-title
  (try
    (bookmark-db/create-bookmark {:bookmark/title   ""
                                  :bookmark/url     "foo"
                                  :bookmark/tab-id  @tab-id
                                  :bookmark/user-id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid bookmark" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/title ["should be at least 1 characters"]}} data)))))

(defexpect fail-create-bookmark-without-url
  (try
    (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                  :bookmark/tab-id  @tab-id
                                  :bookmark/user-id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid bookmark" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/url ["missing required key"]}} data)))))

(defexpect fail-create-bookmark-with-invalid-url
  (try
    (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                  :bookmark/url     ""
                                  :bookmark/tab-id  @tab-id
                                  :bookmark/user-id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid bookmark" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/url ["should be at least 1 characters"]}} data)))))

(defexpect fail-create-bookmark-without-tab
  (try
    (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                  :bookmark/url     "bar"
                                  :bookmark/user-id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid bookmark" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/tab-id ["missing required key"]}} data)))))

(defexpect fail-create-bookmark-with-invalid-tab
  (try
    (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                  :bookmark/url     "bar"
                                  :bookmark/tab-id  "foo"
                                  :bookmark/user-id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid bookmark" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/tab-id ["should be a uuid"]}} data)))))

(defexpect fail-create-bookmark-without-user
  (try
    (bookmark-db/create-bookmark {:bookmark/title  "foo"
                                  :bookmark/url    "bar"
                                  :bookmark/tab-id @tab-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid bookmark" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/user-id ["missing required key"]}} data)))))

(defexpect fail-create-bookmark-with-invalid-user
  (try
    (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                  :bookmark/url     "bar"
                                  :bookmark/user-id @user-id
                                  :bookmark/tab-id  "foo"})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid bookmark" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/tab-id ["should be a uuid"]}} data)))))

(defexpect normal-fetch-bookmark
  (let [bookmark         (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                       :bookmark/url     "bar"
                                                       :bookmark/tab-id  @tab-id
                                                       :bookmark/user-id @user-id})
        fetched-bookmark (bookmark-db/fetch-bookmark {:bookmark/id (:bookmark/id bookmark)
                                                      :user/id     @user-id})]
    (expect bookmark fetched-bookmark)))

(defexpect normal-fetch-nonexistent-bookmark nil (bookmark-db/fetch-bookmark {:bookmark/id (UUID/randomUUID)
                                                                              :user/id     @user-id}))

(defexpect fail-fetch-bookmark-without-id
  (try
    (bookmark-db/fetch-bookmark {:user/id @user-id})
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid bookmark or user ID" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/id ["missing required key"]}} data)))))

(defexpect fail-fetch-bookmark-with-invalid-id
  (try
    (bookmark-db/fetch-bookmark {:bookmark/id "foo" :user/id @user-id})
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid bookmark or user ID" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/id ["should be a uuid"]}} data)))))

(defexpect fail-fetch-bookmark-without-user-id
  (try
    (bookmark-db/fetch-bookmark {:bookmark/id (UUID/randomUUID)})
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid bookmark or user ID" message)
        (expect {:error-type :invalid-input :error-data {:user/id ["missing required key"]}} data)))))

(defexpect fail-fetch-bookmark-with-invalid-user-id
  (try
    (bookmark-db/fetch-bookmark {:bookmark/id (UUID/randomUUID) :user/id "foo"})
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid bookmark or user ID" message)
        (expect {:error-type :invalid-input :error-data {:user/id ["should be a uuid"]}} data)))))

(defexpect normal-fetch-bookmarks
  (let [bookmark1-title   "hello"
        bookmark2-title   "hello"
        bookmark1-url     "world"
        bookmark2-url     "world"
        bookmark1         (bookmark-db/create-bookmark {:bookmark/title   bookmark1-title
                                                        :bookmark/url     bookmark1-url
                                                        :bookmark/tab-id  @tab-id
                                                        :bookmark/user-id @user-id})
        bookmark2         (bookmark-db/create-bookmark {:bookmark/title   bookmark2-title
                                                        :bookmark/url     bookmark2-url
                                                        :bookmark/tab-id  @tab-id
                                                        :bookmark/user-id @user-id})
        fetched-bookmarks (bookmark-db/fetch-bookmarks {:user/id @user-id :tab/id @tab-id})]
    (expect [bookmark1 bookmark2] fetched-bookmarks)))

(defexpect fetch-empty-bookmarks [] (bookmark-db/fetch-bookmarks {:tab/id @tab-id :user/id @user-id}))

(defexpect fail-fetch-bookmarks-without-user-and-tab
  (try
    (bookmark-db/fetch-bookmarks {})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid tab or user ID" message)
        (expect
          {:error-type :invalid-input
           :error-data {:tab/id  ["missing required key"]
                        :user/id ["missing required key"]}}
          data)))))

(defexpect fail-fetch-bookmarks-without-user
  (try
    (bookmark-db/fetch-bookmarks {:tab/id @tab-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid tab or user ID" message)
        (expect {:error-type :invalid-input :error-data {:user/id ["missing required key"]}} data)))))

(defexpect fail-fetch-bookmarks-with-invalid-user
  (try
    (bookmark-db/fetch-bookmarks {:tab/id @tab-id :user/id "foo"})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid tab or user ID" message)
        (expect {:error-type :invalid-input :error-data {:user/id ["should be a uuid"]}} data)))))

(defexpect fail-fetch-bookmarks-without-tab
  (try
    (bookmark-db/fetch-bookmarks {:user/id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid tab or user ID" message)
        (expect {:error-type :invalid-input :error-data {:tab/id ["missing required key"]}} data)))))

(defexpect fail-fetch-bookmarks-with-invalid-tab
  (try
    (bookmark-db/fetch-bookmarks {:tab/id "foo" :user/id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid tab or user ID" message)
        (expect {:error-type :invalid-input :error-data {:tab/id ["should be a uuid"]}} data)))))

(comment
  (require '[kaocha.repl :as k])
  (require '[shinsetsu.db.bookmark-test])
  (k/run 'shinsetsu.db.bookmark-test)
  (k/run #'shinsetsu.db.bookmark-test/normal-fetch-bookmarks)
  )
