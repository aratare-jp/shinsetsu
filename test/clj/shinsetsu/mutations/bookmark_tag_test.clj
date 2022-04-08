(ns shinsetsu.mutations.bookmark-tag-test
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.db.user :as udb]
    [shinsetsu.db.tab :as tdb]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [shinsetsu.db.bookmark :as bdb]
    [shinsetsu.db.tag :as tgdb]
    [shinsetsu.mutations.bookmark-tag :as btm]
    [shinsetsu.parser :refer [protected-parser]]
    [shinsetsu.db.bookmark-tag :as btdb]))

(def user-id (atom nil))
(def tab1-id (atom nil))
(def tab2-id (atom nil))

(defn user-tab-setup
  [f]
  (let [user (udb/create-user {:user/username "john" :user/password "smith"})
        tab1 (tdb/create-tab {:tab/name "foo" :tab/user-id (:user/id user)})
        tab2 (tdb/create-tab {:tab/name "fim" :tab/user-id (:user/id user)})]
    (reset! user-id (:user/id user))
    (reset! tab1-id (:tab/id tab1))
    (reset! tab2-id (:tab/id tab2))
    (f)))

(use-fixtures :once db-setup)
(use-fixtures :each db-cleanup user-tab-setup)

(defexpect normal-create-bookmark-tag
  (let [bookmark     (bdb/create-bookmark {:bookmark/title   "foo"
                                           :bookmark/url     "bar"
                                           :bookmark/user-id @user-id
                                           :bookmark/tab-id  @tab1-id})
        bookmark-id  (:bookmark/id bookmark)
        tag          (tgdb/create-tag {:tag/name "foo" :tag/user-id @user-id})
        tag-id       (:tag/id tag)
        query        [{`(btm/create-bookmark-tag #:bookmark-tag{:bookmark-id ~bookmark-id :tag-id ~tag-id})
                       [:bookmark/id :tag/id]}]
        result       (protected-parser {:request {:user/id @user-id}} query)
        actual       (get result `btm/create-bookmark-tag)
        fetched-tags (btdb/fetch-tags-by-bookmark #:bookmark{:id bookmark-id :user-id @user-id})]
    (expect {:bookmark/id bookmark-id :tag/id tag-id} actual)
    (let [check-fn (fn [fetched]
                     (expect 1 (count fetched))
                     (expect tag-id (-> fetched first :bookmark-tag/tag-id))
                     (expect bookmark-id (-> fetched first :bookmark-tag/bookmark-id))
                     (expect @user-id (-> fetched first :bookmark-tag/user-id)))]
      (check-fn fetched-tags))))

(defexpect fail-create-bookmark-tag-with-invalid-bookmark
  (let [bookmark-id "foo"
        tag         (tgdb/create-tag {:tag/name "foo" :tag/user-id @user-id})
        tag-id      (:tag/id tag)
        query       [{`(btm/create-bookmark-tag #:bookmark-tag{:bookmark-id ~bookmark-id :tag-id ~tag-id})
                      [:bookmark/id :tag/id]}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        error       (get result `btm/create-bookmark-tag)]
    (expect {:error         true
             :error-message "Invalid input"
             :error-type    :invalid-input
             :error-data    {:bookmark-tag/bookmark-id ["should be a uuid"]}} error)))

(defexpect fail-create-bookmark-tag-with-nonexistent-bookmark
  (let [bookmark-id (random-uuid)
        tag         (tgdb/create-tag {:tag/name "foo" :tag/user-id @user-id})
        tag-id      (:tag/id tag)
        query       [{`(btm/create-bookmark-tag #:bookmark-tag{:bookmark-id ~bookmark-id :tag-id ~tag-id})
                      [:bookmark/id :tag/id]}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        error       (get result `btm/create-bookmark-tag)]
    (expect {:error         true
             :error-message "Invalid input"
             :error-type    :invalid-input
             :error-data    {:bookmark-tag/bookmark-id ["nonexistent"] :bookmark-tag/tag-id ["nonexistent"]}} error)))

(defexpect fail-create-bookmark-tag-with-invalid-tag
  (let [bookmark    (bdb/create-bookmark {:bookmark/title   "foo"
                                          :bookmark/url     "bar"
                                          :bookmark/user-id @user-id
                                          :bookmark/tab-id  @tab1-id})
        bookmark-id (:bookmark/id bookmark)
        tag-id      "foo"
        query       [{`(btm/create-bookmark-tag #:bookmark-tag{:bookmark-id ~bookmark-id :tag-id ~tag-id})
                      [:bookmark/id :tag/id]}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        error       (get result `btm/create-bookmark-tag)]
    (expect {:error         true
             :error-message "Invalid input"
             :error-type    :invalid-input
             :error-data    {:bookmark-tag/tag-id ["should be a uuid"]}} error)))

(defexpect fail-create-bookmark-tag-with-nonexistent-tag
  (let [bookmark    (bdb/create-bookmark {:bookmark/title   "foo"
                                          :bookmark/url     "bar"
                                          :bookmark/user-id @user-id
                                          :bookmark/tab-id  @tab1-id})
        bookmark-id (:bookmark/id bookmark)
        tag-id      (random-uuid)
        query       [{`(btm/create-bookmark-tag #:bookmark-tag{:bookmark-id ~bookmark-id :tag-id ~tag-id})
                      [:bookmark/id :tag/id]}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        error       (get result `btm/create-bookmark-tag)]
    (expect {:error         true
             :error-message "Invalid input"
             :error-type    :invalid-input
             :error-data    {:bookmark-tag/bookmark-id ["nonexistent"] :bookmark-tag/tag-id ["nonexistent"]}} error)))


(defexpect normal-delete-bookmark-tag
  (let [bookmark     (bdb/create-bookmark #:bookmark{:title "foo" :url "bar" :user-id @user-id :tab-id @tab1-id})
        bookmark-id  (:bookmark/id bookmark)
        tag          (tgdb/create-tag {:tag/name "foo" :tag/user-id @user-id})
        tag-id       (:tag/id tag)
        _            (btdb/create-bookmark-tag #:bookmark-tag{:bookmark-id bookmark-id :tag-id tag-id :user-id @user-id})
        query        [{`(btm/delete-bookmark-tag #:bookmark-tag{:bookmark-id ~bookmark-id :tag-id ~tag-id})
                       [:bookmark/id :tag/id]}]
        result       (protected-parser {:request {:user/id @user-id}} query)
        bookmark-tag (get result `btm/delete-bookmark-tag)
        fetched-tags (btdb/fetch-tags-by-bookmark #:bookmark{:id bookmark-id :user-id @user-id})]
    (expect [] fetched-tags)
    (expect {:bookmark/id bookmark-id :tag/id tag-id} bookmark-tag)))

(defexpect normal-delete-bookmark-tag-with-nonexistent-bookmark
  (let [bookmark-id (random-uuid)
        tag         (tgdb/create-tag {:tag/name "foo" :tag/user-id @user-id})
        tag-id      (:tag/id tag)
        query       [{`(btm/delete-bookmark-tag #:bookmark-tag{:bookmark-id ~bookmark-id :tag-id ~tag-id})
                      [:bookmark/id :tag/id]}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        actual      (get result `btm/delete-bookmark-tag)]
    (expect {:tag/id tag-id :bookmark/id bookmark-id} actual)))

(defexpect normal-delete-bookmark-tag-with-nonexistent-tag
  (let [bookmark    (bdb/create-bookmark #:bookmark{:title "foo" :url "bar" :user-id @user-id :tab-id @tab1-id})
        bookmark-id (:bookmark/id bookmark)
        tag-id      (random-uuid)
        query       [{`(btm/delete-bookmark-tag #:bookmark-tag{:bookmark-id ~bookmark-id :tag-id ~tag-id})
                      [:bookmark/id :tag/id]}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        actual      (get result `btm/delete-bookmark-tag)]
    (expect {:tag/id tag-id :bookmark/id bookmark-id} actual)))

(defexpect fail-delete-bookmark-tag-with-invalid-bookmark
  (let [bookmark-id "foo"
        tag         (tgdb/create-tag {:tag/name "foo" :tag/user-id @user-id})
        tag-id      (:tag/id tag)
        query       [{`(btm/delete-bookmark-tag #:bookmark-tag{:bookmark-id ~bookmark-id :tag-id ~tag-id})
                      [:bookmark/id :tag/id]}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        error       (get result `btm/delete-bookmark-tag)]
    (expect {:error         true
             :error-message "Invalid input"
             :error-type    :invalid-input
             :error-data    {:bookmark-tag/bookmark-id ["should be a uuid"]}} error)))

(defexpect fail-delete-bookmark-tag-with-invalid-tag
  (let [bookmark    (bdb/create-bookmark #:bookmark{:title "foo" :url "bar" :user-id @user-id :tab-id @tab1-id})
        bookmark-id (:bookmark/id bookmark)
        tag-id      "foo"
        query       [{`(btm/delete-bookmark-tag #:bookmark-tag{:bookmark-id ~bookmark-id :tag-id ~tag-id})
                      [:bookmark/id :tag/id]}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        error       (get result `btm/delete-bookmark-tag)]
    (expect {:error         true
             :error-message "Invalid input"
             :error-type    :invalid-input
             :error-data    {:bookmark-tag/tag-id ["should be a uuid"]}} error)))

(comment
  (require '[kaocha.repl :as k])
  (k/run 'shinsetsu.mutations.bookmark-tag-test))
