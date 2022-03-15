(ns shinsetsu.resolvers.bookmark-test
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.db.bookmark :as bookmark-db]
    [shinsetsu.db.tab :as tab-db]
    [shinsetsu.parser :refer [protected-parser]]
    [taoensso.timbre :as log])
  (:import [java.util UUID]))

(def user-id (atom nil))
(def tab-id (atom nil))

(defn user-tab-setup
  [f]
  (let [user (user-db/create-user {:user/username "john" :user/password "smith"})
        tab  (tab-db/create-tab {:tab/name "foo" :tab/user-id (:user/id user)})]
    (reset! user-id (:user/id user))
    (reset! tab-id (:tab/id tab))
    (f)))

(use-fixtures :once db-setup)
(use-fixtures :each db-cleanup user-tab-setup)

(def bookmark-join [:bookmark/id :bookmark/title :bookmark/url :bookmark/image :bookmark/created :bookmark/updated])

(defn create-bookmark
  [bookmark-title bookmark-url tab-id user-id]
  (-> {:bookmark/title   bookmark-title
       :bookmark/url     bookmark-url
       :bookmark/tab-id  tab-id
       :bookmark/user-id user-id}
      (bookmark-db/create-bookmark)
      (dissoc :bookmark/tab-id)
      (dissoc :bookmark/user-id)))

(defexpect normal-fetch-bookmark
  (let [bookmark       (create-bookmark "foo" "bar" @tab-id @user-id)
        bookmark-id    (:bookmark/id bookmark)
        fetch-bookmark (protected-parser {:request {:user/id @user-id}} [{[:bookmark/id bookmark-id] bookmark-join}])]
    (expect {[:bookmark/id bookmark-id] bookmark} fetch-bookmark)))

(defexpect fetch-empty-bookmark
  (let [random-id (UUID/randomUUID)
        expected  {[:bookmark/id random-id] {:bookmark/id random-id}}
        actual    (protected-parser {:request {:user/id @user-id}} [{[:bookmark/id random-id] bookmark-join}])]
    (expect expected actual)))

(defexpect normal-fetch-bookmarks
  (let [bookmark1 (create-bookmark "foo" "bar" @tab-id @user-id)
        bookmark2 (create-bookmark "foo" "bar" @tab-id @user-id)
        expected  {[:tab/id @tab-id] {:tab/bookmarks [bookmark1 bookmark2]}}
        actual    (protected-parser {:request {:user/id @user-id}} [{[:tab/id @tab-id] [{:tab/bookmarks bookmark-join}]}])]
    (expect expected actual)))

(defexpect fetch-empty-bookmarks
  (let [expected {[:tab/id @tab-id] {:tab/bookmarks []}}
        actual   (protected-parser {:request {:user/id @user-id}} [{[:tab/id @tab-id] [{:tab/bookmarks bookmark-join}]}])]
    (expect expected actual)))

(comment
  (require '[kaocha.repl :as k])
  (require '[shinsetsu.parser :refer [protected-parser]])
  (k/run 'shinsetsu.resolvers.bookmark-test)
  (k/run #'shinsetsu.resolvers.bookmark-test/normal-fetch-bookmark))
