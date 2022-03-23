(ns shinsetsu.resolvers.tag-test
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.db.tag :as tag-db]
    [shinsetsu.db.bookmark :as bookmark-db]
    [shinsetsu.db.tab :as tab-db]
    [shinsetsu.parser :refer [protected-parser]]
    [taoensso.timbre :as log]
    [com.wsscode.pathom.core :as pc])
  (:import [java.util UUID]))

(def user-id (atom nil))
(def tab-id (atom nil))

(defn user-setup
  [f]
  (let [user (user-db/create-user {:user/username "john" :user/password "smith"})
        tab  (tab-db/create-tab {:tab/name "foo" :tab/user-id (:user/id user)})]
    (reset! user-id (:user/id user))
    (reset! tab-id (:tab/id tab))
    (f)))

(use-fixtures :once db-setup)
(use-fixtures :each db-cleanup user-setup)

(def tag-join [:tag/id :tag/name :tag/colour :tag/created :tag/updated])

(defn create-tag
  [tag]
  (-> tag
      (tag-db/create-tag)
      (dissoc :tag/user-id)
      (dissoc :tag/password)))

(defexpect normal-fetch-tag
  (let [tag         (create-tag {:tag/name "foo" :tag/colour "#ffffff" :tag/user-id @user-id})
        tag-id      (:tag/id tag)
        fetched-tag (protected-parser {:request {:user/id @user-id}} [{[:tag/id tag-id] tag-join}])]
    (expect {[:tag/id tag-id] tag} fetched-tag)))

(defexpect normal-fetch-empty-tag
  (let [random-id (UUID/randomUUID)
        expected  {[:tag/id random-id] {:tag/id random-id}}
        actual    (protected-parser {:request {:user/id @user-id}} [{[:tag/id random-id] tag-join}])]
    (expect expected actual)))

(defexpect fail-fetch-invalid-tag
  (let [random-id   "foo"
        inner-error {:error         true
                     :error-type    :invalid-input
                     :error-message "Invalid user or tag ID"
                     :error-data    {:tag/id ["should be a uuid"]}}
        expected    {[:tag/id random-id] {:tag/id      random-id
                                          :tag/name    ::pc/reader-error
                                          :tag/colour  ::pc/reader-error
                                          :tag/created ::pc/reader-error
                                          :tag/updated ::pc/reader-error}
                     ::pc/errors         {[[:tag/id random-id] :tag/name]    inner-error
                                          [[:tag/id random-id] :tag/colour]  inner-error
                                          [[:tag/id random-id] :tag/created] inner-error
                                          [[:tag/id random-id] :tag/updated] inner-error}}
        actual      (protected-parser {:request {:user/id @user-id}} [{[:tag/id random-id] tag-join}])]
    (expect expected actual)))

(defexpect fail-fetch-null-tag
  (let [random-id   nil
        inner-error {:error         true
                     :error-type    :invalid-input
                     :error-message "Invalid user or tag ID"
                     :error-data    {:tag/id ["should be a uuid"]}}
        expected    {[:tag/id random-id] {:tag/id      random-id
                                          :tag/name    ::pc/reader-error
                                          :tag/colour  ::pc/reader-error
                                          :tag/created ::pc/reader-error
                                          :tag/updated ::pc/reader-error}
                     ::pc/errors         {[[:tag/id random-id] :tag/name]    inner-error
                                          [[:tag/id random-id] :tag/colour]  inner-error
                                          [[:tag/id random-id] :tag/created] inner-error
                                          [[:tag/id random-id] :tag/updated] inner-error}}
        actual      (protected-parser {:request {:user/id @user-id}} [{[:tag/id random-id] tag-join}])]
    (expect expected actual)))

(defexpect normal-fetch-tags
  (let [tag1     (create-tag {:tag/name "foo" :tag/colour "#fff" :tag/user-id @user-id})
        tag2     (create-tag {:tag/name "foo1" :tag/colour "#000000" :tag/user-id @user-id})
        tag3     (create-tag {:tag/name "foo1" :tag/colour "#fafafa" :tag/user-id @user-id})
        expected {:user/tags [tag1 tag2 tag3]}
        actual   (protected-parser {:request {:user/id @user-id}} [{:user/tags tag-join}])]
    (expect expected actual)))

(defexpect normal-fetch-empty-tags
  (let [expected {:user/tags []}
        actual   (protected-parser {:request {:user/id @user-id}} [{:user/tags tag-join}])]
    (expect expected actual)))

(defexpect normal-fetch-bookmark-tags
  (let [bookmark    (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                  :bookmark/url     "bar"
                                                  :bookmark/tab-id  @tab-id
                                                  :bookmark/user-id @user-id})
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
  (let [bookmark    (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                  :bookmark/url     "bar"
                                                  :bookmark/tab-id  @tab-id
                                                  :bookmark/user-id @user-id})
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
  (k/run 'shinsetsu.resolvers.tag-test)
  (k/run #'shinsetsu.resolvers.tag-test/normal-fetch-tags))
