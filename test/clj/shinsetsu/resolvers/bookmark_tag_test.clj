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
    [shinsetsu.db.bookmark-tag :as btdb]))

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

(defexpect ^:resolver ^:tag ^:integration normal-fetch-tags-by-bookmark
  (let [bookmark    (create-bookmark "foo" "bar" @tab-id @user-id)
        bookmark-id (:bookmark/id bookmark)
        tag         (create-tag "foo" @user-id)
        tag-id      (:tag/id tag)
        _           (btdb/create-bookmark-tag #:bookmark-tag{:bookmark-id bookmark-id :tag-id tag-id :user-id @user-id})
        query       [{[:bookmark/id bookmark-id] [:bookmark/tags]}]
        actual      (protected-parser {:request {:user/id @user-id}} query)
        expected    {[:bookmark/id bookmark-id]
                     {:bookmark/tags (->> #:bookmark{:id bookmark-id :user-id @user-id}
                                          btdb/fetch-tags-by-bookmark
                                          (mapv (fn [bt] {:tag/id (:bookmark-tag/tag-id bt)})))}}]
    (expect expected actual)))

(defexpect ^:resolver ^:tag ^:integration normal-fetch-empty-bookmark-tags
  (let [bookmark-id (random-uuid)
        query       [{[:bookmark/id bookmark-id] [:bookmark/tags]}]
        actual      (protected-parser {:request {:user/id @user-id}} query)
        expected    {[:bookmark/id bookmark-id] {:bookmark/tags []}}]
    (expect expected actual)))

(defexpect ^:resolver ^:tag ^:integration fail-fetch-invalid-bookmark-tags
  (let [bookmark-id "foo"
        expected    {[:bookmark/id bookmark-id] {:error         true
                                                 :error-type    :invalid-input
                                                 :error-message "Invalid input"
                                                 :error-data    {:bookmark/id ["should be a uuid"]}}}
        query       [{[:bookmark/id bookmark-id] [:bookmark/tags]}]
        actual      (protected-parser {:request {:user/id @user-id}} query)]
    (expect expected actual)))

(comment
  (require '[kaocha.repl :as k])
  (require '[shinsetsu.parser :refer [protected-parser]])
  (k/run 'shinsetsu.resolvers.bookmark-tag-test)
  (k/run #'shinsetsu.resolvers.bookmark-tag-test/fail-fetch-invalid-bookmark-tags))
