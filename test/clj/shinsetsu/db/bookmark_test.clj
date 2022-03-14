(ns shinsetsu.db.bookmark-test
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.db.test-utility :refer [db-setup db-cleanup]]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.db.tab :as tab-db]
    [shinsetsu.db.bookmark :as bookmark-db])
  (:import [org.postgresql.util PSQLException]))

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

(defexpect fail-create-bookmark-without-title PSQLException (bookmark-db/create-bookmark {:bookmark/url     "foo"
                                                                                          :bookmark/tab-id  @tab-id
                                                                                          :bookmark/user-id @user-id}))

(defexpect fail-create-bookmark-without-url PSQLException (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                                                        :bookmark/tab-id  @tab-id
                                                                                        :bookmark/user-id @user-id}))

(defexpect fail-create-bookmark-without-tab PSQLException (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                                                        :bookmark/url     "bar"
                                                                                        :bookmark/user-id @user-id}))

(defexpect fail-create-bookmark-without-user PSQLException (bookmark-db/create-bookmark {:bookmark/title  "foo"
                                                                                         :bookmark/url    "bar"
                                                                                         :bookmark/tab-id @tab-id}))

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
(defexpect fetch-nonexistent-bookmarks [] (bookmark-db/fetch-bookmarks {}))

(comment
  (require '[kaocha.repl :as k])
  (k/run 'shinsetsu.db.bookmark-test)
  (k/run #'shinsetsu.db.bookmark-test/normal-fetch-bookmarks)
  )
