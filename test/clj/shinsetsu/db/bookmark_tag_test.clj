(ns shinsetsu.db.bookmark-tag-test
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.db.bookmark :as bookmark-db]
    [shinsetsu.db.tag :as tag-db]
    [shinsetsu.db.bookmark-tag :as bookmark-tag-db]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.db.tab :as tab-db]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]])
  (:import [java.util UUID]))

(def user (atom nil))
(def user-id (atom nil))
(def tab1 (atom nil))
(def tab1-id (atom nil))
(def tab2 (atom nil))
(def tab2-id (atom nil))

(defn user-tab-setup
  [f]
  (reset! user (user-db/create-user {:user/username "foo" :user/password "bar"}))
  (reset! user-id (:user/id @user))
  (reset! tab1 (tab-db/create-tab {:tab/name "foo" :tab/user-id @user-id}))
  (reset! tab1-id (:tab/id @tab1))
  (reset! tab2 (tab-db/create-tab {:tab/name "baz" :tab/user-id @user-id}))
  (reset! tab2-id (:tab/id @tab2))
  (f))

(use-fixtures :once db-setup)
(use-fixtures :each db-cleanup user-tab-setup)

(defexpect normal-create-bookmark-tag
  (let [bookmark     (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                   :bookmark/url     "bar"
                                                   :bookmark/user-id @user-id
                                                   :bookmark/tab-id  @tab1-id})
        bookmark-id  (:bookmark/id bookmark)
        tag          (tag-db/create-tag {:tag/name "bob" :tag/user-id @user-id})
        tag-id       (:tag/id tag)
        bookmark-tag (bookmark-tag-db/create-bookmark-tag #:bookmark-tag{:bookmark-id bookmark-id
                                                                         :tag-id      tag-id
                                                                         :user-id     @user-id})]
    (expect bookmark-id (:bookmark-tag/bookmark-id bookmark-tag))
    (expect tag-id (:bookmark-tag/tag-id bookmark-tag))
    (expect @user-id (:bookmark-tag/user-id bookmark-tag))))

(defexpect fail-create-bookmark-tag-with-invalid-bookmark-id
  (try
    (let [bookmark-id "foo"
          tag         (tag-db/create-tag {:tag/name "bob" :tag/user-id @user-id})
          tag-id      (:tag/id tag)]
      (bookmark-tag-db/create-bookmark-tag #:bookmark-tag{:bookmark-id bookmark-id :tag-id tag-id :user-id @user-id})
      (expect false))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark-tag/bookmark-id ["should be a uuid"]}} data)))))

(defexpect fail-create-bookmark-tag-with-invalid-tag-id
  (try
    (let [bookmark    (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                    :bookmark/url     "bar"
                                                    :bookmark/user-id @user-id
                                                    :bookmark/tab-id  @tab1-id})
          bookmark-id (:bookmark/id bookmark)
          tag-id      "foo"]
      (bookmark-tag-db/create-bookmark-tag #:bookmark-tag{:bookmark-id bookmark-id :tag-id tag-id :user-id @user-id})
      (expect false))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark-tag/tag-id ["should be a uuid"]}} data)))))

(defexpect normal-fetch-bookmarks-by-tag
  (let [bookmark             (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                           :bookmark/url     "bar"
                                                           :bookmark/user-id @user-id
                                                           :bookmark/tab-id  @tab1-id})
        bookmark-id          (:bookmark/id bookmark)
        tag                  (tag-db/create-tag {:tag/name "bob" :tag/user-id @user-id})
        tag-id               (:tag/id tag)
        bookmark-tag         (bookmark-tag-db/create-bookmark-tag #:bookmark-tag{:bookmark-id bookmark-id
                                                                                 :tag-id      tag-id
                                                                                 :user-id     @user-id})
        fetched-bookmark-tag (bookmark-tag-db/fetch-bookmarks-by-tag #:bookmark-tag{:tag-id tag-id :user-id @user-id})]
    (expect [bookmark-tag] fetched-bookmark-tag)))

(defexpect normal-fetch-empty-bookmarks-by-tag
  (let [tag                  (tag-db/create-tag {:tag/name "bob" :tag/user-id @user-id})
        tag-id               (:tag/id tag)
        fetched-bookmark-tag (bookmark-tag-db/fetch-bookmarks-by-tag #:bookmark-tag{:tag-id tag-id :user-id @user-id})]
    (expect [] fetched-bookmark-tag)))

(defexpect normal-fetch-bookmarks-by-nonexistent-tag
  (let [fetched-bookmark-tag (bookmark-tag-db/fetch-bookmarks-by-tag #:bookmark-tag{:tag-id  (UUID/randomUUID)
                                                                                    :user-id @user-id})]
    (expect [] fetched-bookmark-tag)))

(defexpect fail-fetch-bookmarks-by-invalid-tag
  (try
    (bookmark-tag-db/fetch-bookmarks-by-tag #:bookmark-tag{:tag-id "foo" :user-id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark-tag/tag-id ["should be a uuid"]}} data)))))

(defexpect normal-fetch-tags-by-bookmark
  (let [bookmark             (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                           :bookmark/url     "bar"
                                                           :bookmark/user-id @user-id
                                                           :bookmark/tab-id  @tab1-id})
        bookmark-id          (:bookmark/id bookmark)
        tag                  (tag-db/create-tag {:tag/name "bob" :tag/user-id @user-id})
        tag-id               (:tag/id tag)
        bookmark-tag         (bookmark-tag-db/create-bookmark-tag #:bookmark-tag{:bookmark-id bookmark-id
                                                                                 :tag-id      tag-id
                                                                                 :user-id     @user-id})
        fetched-bookmark-tag (bookmark-tag-db/fetch-tags-by-bookmark #:bookmark-tag{:bookmark-id bookmark-id
                                                                                    :user-id     @user-id})]
    (expect [bookmark-tag] fetched-bookmark-tag)))

(defexpect normal-fetch-empty-tags-by-bookmark
  (let [bookmark             (bookmark-db/create-bookmark {:bookmark/title   "bob"
                                                           :bookmark/url     "bar"
                                                           :bookmark/tab-id  @tab1-id
                                                           :bookmark/user-id @user-id})
        bookmark-id          (:bookmark/id bookmark)
        fetched-bookmark-tag (bookmark-tag-db/fetch-tags-by-bookmark #:bookmark-tag{:bookmark-id bookmark-id
                                                                                    :user-id     @user-id})]
    (expect [] fetched-bookmark-tag)))

(defexpect normal-fetch-tags-by-nonexistent-bookmark
  (let [fetched-bookmark-tag (bookmark-tag-db/fetch-tags-by-bookmark #:bookmark-tag{:bookmark-id (UUID/randomUUID)
                                                                                    :user-id     @user-id})]
    (expect [] fetched-bookmark-tag)))

(defexpect fail-fetch-tags-by-invalid-bookmark
  (try
    (bookmark-tag-db/fetch-tags-by-bookmark #:bookmark-tag{:bookmark-id "foo" :user-id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark-tag/bookmark-id ["should be a uuid"]}} data)))))


(defexpect normal-delete-bookmark-tag
  (let [bookmark             (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                           :bookmark/url     "bar"
                                                           :bookmark/user-id @user-id
                                                           :bookmark/tab-id  @tab1-id})
        bookmark-id          (:bookmark/id bookmark)
        tag                  (tag-db/create-tag {:tag/name "bob" :tag/user-id @user-id})
        tag-id               (:tag/id tag)
        bookmark-tag         (bookmark-tag-db/create-bookmark-tag #:bookmark-tag{:bookmark-id bookmark-id
                                                                                 :tag-id      tag-id
                                                                                 :user-id     @user-id})
        deleted-bookmark-tag (bookmark-tag-db/delete-bookmark-tag #:bookmark-tag{:bookmark-id bookmark-id
                                                                                 :tag-id      tag-id
                                                                                 :user-id     @user-id})
        fetched-bookmark-tag (bookmark-tag-db/fetch-bookmarks-by-tag #:bookmark-tag{:tag-id tag-id :user-id @user-id})]
    (expect [] fetched-bookmark-tag)
    (expect bookmark-tag deleted-bookmark-tag)))

(defexpect normal-delete-bookmark-tag-by-nonexistent-bookmark
  (let [bookmark-id          (UUID/randomUUID)
        tag                  (tag-db/create-tag {:tag/name "bob" :tag/user-id @user-id})
        tag-id               (:tag/id tag)
        deleted-bookmark-tag (bookmark-tag-db/delete-bookmark-tag #:bookmark-tag{:bookmark-id bookmark-id
                                                                                 :tag-id      tag-id
                                                                                 :user-id     @user-id})]
    (expect nil deleted-bookmark-tag)))

(defexpect normal-delete-bookmark-tag-by-nonexistent-tag
  (let [bookmark             (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                           :bookmark/url     "bar"
                                                           :bookmark/user-id @user-id
                                                           :bookmark/tab-id  @tab1-id})
        bookmark-id          (:bookmark/id bookmark)
        tag-id               (UUID/randomUUID)
        deleted-bookmark-tag (bookmark-tag-db/delete-bookmark-tag #:bookmark-tag{:bookmark-id bookmark-id
                                                                                 :tag-id      tag-id
                                                                                 :user-id     @user-id})]
    (expect nil deleted-bookmark-tag)))

(defexpect fail-delete-bookmark-tag-by-invalid-tag
  (try
    (bookmark-tag-db/delete-bookmark-tag #:bookmark-tag{:tag-id "foo" :bookmark-id (UUID/randomUUID) :user-id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark-tag/tag-id ["should be a uuid"]}} data)))))

(defexpect fail-delete-bookmark-tag-by-invalid-bookmark
  (try
    (bookmark-tag-db/delete-bookmark-tag #:bookmark-tag{:tag-id (UUID/randomUUID) :bookmark-id "foo" :user-id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark-tag/bookmark-id ["should be a uuid"]}} data)))))

(comment
  (require '[kaocha.repl :as k])
  (k/run 'shinsetsu.db.bookmark-tag-test))
