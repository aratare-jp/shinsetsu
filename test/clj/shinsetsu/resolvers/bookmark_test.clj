(ns shinsetsu.resolvers.bookmark-test
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.db.bookmark :as bookmark-db]
    [shinsetsu.db.tab :as tab-db]
    [shinsetsu.db.tag :as tag-db]
    [shinsetsu.parser :refer [protected-parser]]
    [taoensso.timbre :as log]
    [com.wsscode.pathom.core :as pc])
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

(defexpect normal-fetch-empty-bookmark
  (let [random-id (UUID/randomUUID)
        expected  {[:bookmark/id random-id] {:bookmark/id random-id}}
        actual    (protected-parser {:request {:user/id @user-id}} [{[:bookmark/id random-id] bookmark-join}])]
    (expect expected actual)))

(defexpect fail-fetch-invalid-bookmark
  (let [random-id   "foo"
        inner-error {:error         true
                     :error-type    :invalid-input
                     :error-message "Invalid bookmark or user ID"
                     :error-data    {:bookmark/id ["should be a uuid"]}}
        expected    {[:bookmark/id random-id] {:bookmark/id      random-id
                                               :bookmark/title   ::pc/reader-error
                                               :bookmark/url     ::pc/reader-error
                                               :bookmark/image   ::pc/reader-error
                                               :bookmark/created ::pc/reader-error
                                               :bookmark/updated ::pc/reader-error}
                     ::pc/errors              {[[:bookmark/id random-id] :bookmark/title]   inner-error
                                               [[:bookmark/id random-id] :bookmark/url]     inner-error
                                               [[:bookmark/id random-id] :bookmark/image]   inner-error
                                               [[:bookmark/id random-id] :bookmark/created] inner-error
                                               [[:bookmark/id random-id] :bookmark/updated] inner-error}}
        actual      (protected-parser {:request {:user/id @user-id}} [{[:bookmark/id random-id] bookmark-join}])]
    (expect expected actual)))

(defexpect fail-fetch-null-bookmark
  (let [random-id   nil
        inner-error {:error         true
                     :error-type    :invalid-input
                     :error-message "Invalid bookmark or user ID"
                     :error-data    {:bookmark/id ["should be a uuid"]}}
        expected    {[:bookmark/id random-id] {:bookmark/id      random-id
                                               :bookmark/title   ::pc/reader-error
                                               :bookmark/url     ::pc/reader-error
                                               :bookmark/image   ::pc/reader-error
                                               :bookmark/created ::pc/reader-error
                                               :bookmark/updated ::pc/reader-error}
                     ::pc/errors              {[[:bookmark/id random-id] :bookmark/title]   inner-error
                                               [[:bookmark/id random-id] :bookmark/url]     inner-error
                                               [[:bookmark/id random-id] :bookmark/image]   inner-error
                                               [[:bookmark/id random-id] :bookmark/created] inner-error
                                               [[:bookmark/id random-id] :bookmark/updated] inner-error}}
        actual      (protected-parser {:request {:user/id @user-id}} [{[:bookmark/id random-id] bookmark-join}])]
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

(defexpect fail-fetch-invalid-bookmarks
  (let [random-id   "foo"
        inner-error {:error         true
                     :error-type    :invalid-input
                     :error-message "Invalid tab or user ID"
                     :error-data    {:tab/id ["should be a uuid"]}}
        expected    {[:tab/id random-id] {:tab/bookmarks ::pc/reader-error}
                     ::pc/errors         {[[:tab/id random-id] :tab/bookmarks] inner-error}}
        actual      (protected-parser {:request {:user/id @user-id}} [{[:tab/id random-id] [:tab/bookmarks]}])]
    (expect expected actual)))

(defexpect normal-fetch-bookmark-tags
  (let [bookmark    (create-bookmark "foo" "bar" @tab-id @user-id)
        bookmark-id (:bookmark/id bookmark)
        tag1        (tag-db/create-tag {:tag/name "foo" :tag/colour "#ffffff" :tag/user-id @user-id})
        tag1-id     (:tag/id tag1)
        tag2        (tag-db/create-tag {:tag/name "foo" :tag/colour "#ffffff" :tag/user-id @user-id})
        tag2-id     (:tag/id tag2)
        _           (bookmark-db/create-bookmark-tag {:bookmark/id bookmark-id :tag/id tag1-id :user/id @user-id})
        _           (bookmark-db/create-bookmark-tag {:bookmark/id bookmark-id :tag/id tag2-id :user/id @user-id})
        expected    {[:bookmark/id bookmark-id] {:bookmark/tags [{:tag/id tag1-id} {:tag/id tag2-id}]}}
        actual      (protected-parser {:request {:user/id @user-id}} [{[:bookmark/id bookmark-id] [:bookmark/tags]}])]
    (expect expected actual)))

(defexpect normal-fetch-empty-bookmark-tags
  (let [bookmark    (create-bookmark "foo" "bar" @tab-id @user-id)
        bookmark-id (:bookmark/id bookmark)
        expected    {[:bookmark/id bookmark-id] {:bookmark/tags []}}
        actual      (protected-parser {:request {:user/id @user-id}} [{[:bookmark/id bookmark-id] [:bookmark/tags]}])]
    (expect expected actual)))

(defexpect fail-fetch-invalid-bookmark-tags
  (let [random-id   "foo"
        inner-error {:error         true
                     :error-type    :invalid-input
                     :error-message "Invalid bookmark or user ID"
                     :error-data    {:bookmark/id ["should be a uuid"]}}
        expected    {[:bookmark/id random-id] {:bookmark/tags ::pc/reader-error}
                     ::pc/errors              {[[:bookmark/id random-id] :bookmark/tags] inner-error}}
        actual      (protected-parser {:request {:user/id @user-id}} [{[:bookmark/id random-id] [:bookmark/tags]}])]
    (expect expected actual)))

(comment
  (require '[kaocha.repl :as k])
  (require '[shinsetsu.parser :refer [protected-parser]])
  (k/run 'shinsetsu.resolvers.bookmark-test)
  (k/run #'shinsetsu.resolvers.bookmark-test/fail-fetch-invalid-bookmark))
