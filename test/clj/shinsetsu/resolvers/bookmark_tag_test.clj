(ns shinsetsu.resolvers.bookmark-tag-test
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
    [com.wsscode.pathom.core :as pc]
    [shinsetsu.db.bookmark-tag :as bookmark-tag-db])
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

(defn create-bookmark
  [title url tab-id user-id]
  (-> #:bookmark{:title title :url url :tab-id tab-id :user-id user-id}
      bookmark-db/create-bookmark
      (dissoc :bookmark/tab-id)
      (dissoc :bookmark/user-id)))

(defn create-tag
  [name user-id]
  (-> #:tag{:name name :user-id user-id}
      tag-db/create-tag
      (dissoc :tag/user-id)))

(defexpect normal-fetch-tags-by-bookmark
  (let [bookmark     (create-bookmark "foo" "bar" @tab-id @user-id)
        bookmark-id  (:bookmark/id bookmark)
        tag          (create-tag "foo" @user-id)
        tag-id       (:tag/id tag)
        bookmark-tag (bookmark-tag-db/create-bookmark-tag #:bookmark-tag{:bookmark-id bookmark-id
                                                                         :tag-id      tag-id
                                                                         :user-id     @user-id})
        query        [{[:bookmark-tag/bookmark-id bookmark-id] [:bookmark/tags]}]
        actual       (protected-parser {:request {:user/id @user-id}} query)
        expected     (->> (bookmark-tag-db/fetch-tags-by-bookmark #:bookmark-tag{:bookmark-id bookmark-id :user-id @user-id})
                          (mapv (fn [bt] {:tag/id (:bookmark-tag/tag-id bt)})))]
    (expect {[:bookmark-tag/bookmark-id bookmark-id] {:bookmark/tags expected}} actual)))

(defexpect normal-fetch-bookmarks-by-tag
  (let [bookmark     (create-bookmark "foo" "bar" @tab-id @user-id)
        bookmark-id  (:bookmark/id bookmark)
        tag          (create-tag "foo" @user-id)
        tag-id       (:tag/id tag)
        bookmark-tag (bookmark-tag-db/create-bookmark-tag #:bookmark-tag{:bookmark-id bookmark-id
                                                                         :tag-id      tag-id
                                                                         :user-id     @user-id})
        query        [{[:bookmark-tag/tag-id tag-id] [:tag/bookmarks]}]
        actual       (protected-parser {:request {:user/id @user-id}} query)
        expected     (->> (bookmark-tag-db/fetch-bookmarks-by-tag #:bookmark-tag{:tag-id tag-id :user-id @user-id})
                          (mapv (fn [bt] {:bookmark/id (:bookmark-tag/bookmark-id bt)})))]
    (expect {[:bookmark-tag/tag-id tag-id] {:tag/bookmarks expected}} actual)))

(defexpect normal-fetch-empty-bookmark
  (let [bookmark-id (UUID/randomUUID)
        query       [{[:bookmark-tag/bookmark-id bookmark-id] [:bookmark/tags]}]
        actual      (protected-parser {:request {:user/id @user-id}} query)]
    (expect {[:bookmark-tag/bookmark-id bookmark-id] {:bookmark/tags []}} actual)))

(defexpect normal-fetch-empty-tag
  (let [tag-id (UUID/randomUUID)
        query  [{[:bookmark-tag/tag-id tag-id] [:tag/bookmarks]}]
        actual (protected-parser {:request {:user/id @user-id}} query)]
    (expect {[:bookmark-tag/tag-id tag-id] {:tag/bookmarks []}} actual)))

(defexpect fail-fetch-invalid-tags-by-bookmark
  (let [bookmark-id "foo"
        inner-error {:error         true
                     :error-type    :invalid-input
                     :error-message "Invalid input"
                     :error-data    {:bookmark-tag/bookmark-id ["should be a uuid"]}}
        expected    {[:bookmark-tag/bookmark-id bookmark-id] {:bookmark/tags ::pc/reader-error}
                     ::pc/errors                             {[[:bookmark-tag/bookmark-id bookmark-id] :bookmark/tags] inner-error}}
        query       [{[:bookmark-tag/bookmark-id bookmark-id] [:bookmark/tags]}]
        actual      (protected-parser {:request {:user/id @user-id}} query)]
    (expect expected actual)))

(defexpect fail-fetch-invalid-bookmarks-by-tag
  (let [tag-id      "foo"
        inner-error {:error         true
                     :error-type    :invalid-input
                     :error-message "Invalid input"
                     :error-data    {:bookmark-tag/tag-id ["should be a uuid"]}}
        expected    {[:bookmark-tag/tag-id tag-id] {:tag/bookmarks ::pc/reader-error}
                     ::pc/errors                   {[[:bookmark-tag/tag-id tag-id] :tag/bookmarks] inner-error}}
        query       [{[:bookmark-tag/tag-id tag-id] [:tag/bookmarks]}]
        actual      (protected-parser {:request {:user/id @user-id}} query)]
    (expect expected actual)))

(comment
  (require '[kaocha.repl :as k])
  (require '[shinsetsu.parser :refer [protected-parser]])
  (k/run 'shinsetsu.resolvers.bookmark-tag-test)
  (k/run #'shinsetsu.resolvers.bookmark-tag-test/fail-fetch-invalid-tags-by-bookmark))
