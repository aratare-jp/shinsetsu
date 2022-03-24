(ns shinsetsu.mutations.bookmark-tag-test
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.db.tab :as tab-db]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [shinsetsu.db.bookmark :as bookmark-db]
    [shinsetsu.db.tag :as tag-db]
    [shinsetsu.mutations.bookmark-tag :as bookmark-tag-mut]
    [shinsetsu.parser :refer [protected-parser]]
    [shinsetsu.db.bookmark-tag :as bookmark-tag-db])
  (:import [java.util UUID]))

(def user-id (atom nil))
(def tab1-id (atom nil))
(def tab2-id (atom nil))

(defn user-tab-setup
  [f]
  (let [user (user-db/create-user {:user/username "john" :user/password "smith"})
        tab1 (tab-db/create-tab {:tab/name "foo" :tab/user-id (:user/id user)})
        tab2 (tab-db/create-tab {:tab/name "fim" :tab/user-id (:user/id user)})]
    (reset! user-id (:user/id user))
    (reset! tab1-id (:tab/id tab1))
    (reset! tab2-id (:tab/id tab2))
    (f)))

(use-fixtures :once db-setup)
(use-fixtures :each db-cleanup user-tab-setup)

(defexpect normal-create-bookmark-tag
  (let [bookmark     (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                   :bookmark/url     "bar"
                                                   :bookmark/user-id @user-id
                                                   :bookmark/tab-id  @tab1-id})
        bookmark-id  (:bookmark/id bookmark)
        tag          (tag-db/create-tag {:tag/name "foo" :tag/user-id @user-id})
        tag-id       (:tag/id tag)
        query        [{`(bookmark-tag-mut/create-bookmark-tag #:bookmark-tag{:bookmark-id ~bookmark-id :tag-id ~tag-id})
                       [:bookmark/id :tag/id]}]
        result       (protected-parser {:request {:user/id @user-id}} query)
        actual       (get result `bookmark-tag-mut/create-bookmark-tag)
        fetched-tags (bookmark-tag-db/fetch-tags-by-bookmark #:bookmark-tag{:bookmark-id bookmark-id :user-id @user-id})
        fetched-bms  (bookmark-tag-db/fetch-bookmarks-by-tag #:bookmark-tag{:tag-id tag-id :user-id @user-id})]
    (expect {:bookmark/id bookmark-id :tag/id tag-id} actual)
    (let [check-fn (fn [fetched]
                     (expect 1 (count fetched))
                     (expect tag-id (-> fetched first :bookmark-tag/tag-id))
                     (expect bookmark-id (-> fetched first :bookmark-tag/bookmark-id))
                     (expect @user-id (-> fetched first :bookmark-tag/user-id)))]
      (check-fn fetched-tags)
      (check-fn fetched-bms))))

(defexpect fail-create-bookmark-tag-with-invalid-bookmark
  (let [bookmark-id "foo"
        tag         (tag-db/create-tag {:tag/name "foo" :tag/user-id @user-id})
        tag-id      (:tag/id tag)
        query       [{`(bookmark-tag-mut/create-bookmark-tag #:bookmark-tag{:bookmark-id ~bookmark-id :tag-id ~tag-id})
                      [:bookmark/id :tag/id]}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        error       (get result `bookmark-tag-mut/create-bookmark-tag)]
    (expect {:error         true
             :error-message "Invalid input"
             :error-type    :invalid-input
             :error-data    {:bookmark-tag/bookmark-id ["should be a uuid"]}} error)))

(defexpect fail-create-bookmark-tag-with-nonexistent-bookmark
  (let [bookmark-id (UUID/randomUUID)
        tag         (tag-db/create-tag {:tag/name "foo" :tag/user-id @user-id})
        tag-id      (:tag/id tag)
        query       [{`(bookmark-tag-mut/create-bookmark-tag #:bookmark-tag{:bookmark-id ~bookmark-id :tag-id ~tag-id})
                      [:bookmark/id :tag/id]}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        error       (get result `bookmark-tag-mut/create-bookmark-tag)]
    (expect {:error         true
             :error-message "Invalid input"
             :error-type    :invalid-input
             :error-data    {:bookmark-tag/bookmark-id ["nonexistent"] :bookmark-tag/tag-id ["nonexistent"]}} error)))

(defexpect fail-create-bookmark-tag-with-invalid-tag
  (let [bookmark    (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                  :bookmark/url     "bar"
                                                  :bookmark/user-id @user-id
                                                  :bookmark/tab-id  @tab1-id})
        bookmark-id (:bookmark/id bookmark)
        tag-id      "foo"
        query       [{`(bookmark-tag-mut/create-bookmark-tag #:bookmark-tag{:bookmark-id ~bookmark-id :tag-id ~tag-id})
                      [:bookmark/id :tag/id]}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        error       (get result `bookmark-tag-mut/create-bookmark-tag)]
    (expect {:error         true
             :error-message "Invalid input"
             :error-type    :invalid-input
             :error-data    {:bookmark-tag/tag-id ["should be a uuid"]}} error)))

(defexpect fail-create-bookmark-tag-with-nonexistent-tag
  (let [bookmark    (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                  :bookmark/url     "bar"
                                                  :bookmark/user-id @user-id
                                                  :bookmark/tab-id  @tab1-id})
        bookmark-id (:bookmark/id bookmark)
        tag-id      (UUID/randomUUID)
        query       [{`(bookmark-tag-mut/create-bookmark-tag #:bookmark-tag{:bookmark-id ~bookmark-id :tag-id ~tag-id})
                      [:bookmark/id :tag/id]}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        error       (get result `bookmark-tag-mut/create-bookmark-tag)]
    (expect {:error         true
             :error-message "Invalid input"
             :error-type    :invalid-input
             :error-data    {:bookmark-tag/bookmark-id ["nonexistent"] :bookmark-tag/tag-id ["nonexistent"]}} error)))


(defexpect normal-delete-bookmark-tag
  (let [bookmark     (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                   :bookmark/url     "bar"
                                                   :bookmark/user-id @user-id
                                                   :bookmark/tab-id  @tab1-id})
        bookmark-id  (:bookmark/id bookmark)
        tag          (tag-db/create-tag {:tag/name "foo" :tag/user-id @user-id})
        tag-id       (:tag/id tag)
        _            (bookmark-tag-db/create-bookmark-tag #:bookmark-tag{:bookmark-id bookmark-id
                                                                         :tag-id      tag-id
                                                                         :user-id     @user-id})
        query        [{`(bookmark-tag-mut/delete-bookmark-tag #:bookmark-tag{:bookmark-id ~bookmark-id :tag-id ~tag-id})
                       [:bookmark/id :tag/id]}]
        result       (protected-parser {:request {:user/id @user-id}} query)
        bookmark-tag (get result `bookmark-tag-mut/delete-bookmark-tag)
        fetched-tags (bookmark-tag-db/fetch-tags-by-bookmark #:bookmark-tag{:bookmark-id bookmark-id :user-id @user-id})
        fetched-bms  (bookmark-tag-db/fetch-bookmarks-by-tag #:bookmark-tag{:tag-id tag-id :user-id @user-id})]
    (expect [] fetched-tags)
    (expect [] fetched-bms)
    (expect {:bookmark/id bookmark-id :tag/id tag-id} bookmark-tag)))

(defexpect normal-delete-bookmark-tag-with-nonexistent-bookmark
  (let [bookmark-id (UUID/randomUUID)
        tag         (tag-db/create-tag {:tag/name "foo" :tag/user-id @user-id})
        tag-id      (:tag/id tag)
        query       [{`(bookmark-tag-mut/delete-bookmark-tag #:bookmark-tag{:bookmark-id ~bookmark-id :tag-id ~tag-id})
                      [:bookmark/id :tag/id]}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        actual      (get result `bookmark-tag-mut/delete-bookmark-tag)]
    (expect {:tag/id tag-id :bookmark/id bookmark-id} actual)))

(defexpect normal-delete-bookmark-tag-with-nonexistent-tag
  (let [bookmark    (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                  :bookmark/url     "bar"
                                                  :bookmark/user-id @user-id
                                                  :bookmark/tab-id  @tab1-id})
        bookmark-id (:bookmark/id bookmark)
        tag-id      (UUID/randomUUID)
        query       [{`(bookmark-tag-mut/delete-bookmark-tag #:bookmark-tag{:bookmark-id ~bookmark-id :tag-id ~tag-id})
                      [:bookmark/id :tag/id]}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        actual      (get result `bookmark-tag-mut/delete-bookmark-tag)]
    (expect {:tag/id tag-id :bookmark/id bookmark-id} actual)))

(defexpect fail-delete-bookmark-tag-with-invalid-bookmark
  (let [bookmark-id "foo"
        tag         (tag-db/create-tag {:tag/name "foo" :tag/user-id @user-id})
        tag-id      (:tag/id tag)
        query       [{`(bookmark-tag-mut/delete-bookmark-tag #:bookmark-tag{:bookmark-id ~bookmark-id :tag-id ~tag-id})
                      [:bookmark/id :tag/id]}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        error       (get result `bookmark-tag-mut/delete-bookmark-tag)]
    (expect {:error         true
             :error-message "Invalid input"
             :error-type    :invalid-input
             :error-data    {:bookmark-tag/bookmark-id ["should be a uuid"]}} error)))

(defexpect fail-delete-bookmark-tag-with-invalid-tag
  (let [bookmark    (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                  :bookmark/url     "bar"
                                                  :bookmark/user-id @user-id
                                                  :bookmark/tab-id  @tab1-id})
        bookmark-id (:bookmark/id bookmark)
        tag-id      "foo"
        query       [{`(bookmark-tag-mut/delete-bookmark-tag #:bookmark-tag{:bookmark-id ~bookmark-id :tag-id ~tag-id})
                      [:bookmark/id :tag/id]}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        error       (get result `bookmark-tag-mut/delete-bookmark-tag)]
    (expect {:error         true
             :error-message "Invalid input"
             :error-type    :invalid-input
             :error-data    {:bookmark-tag/tag-id ["should be a uuid"]}} error)))

(comment
  (require '[kaocha.repl :as k])
  (k/run 'shinsetsu.mutations.bookmark-tag-test))
