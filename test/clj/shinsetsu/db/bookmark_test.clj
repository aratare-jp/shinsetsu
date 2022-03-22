(ns shinsetsu.db.bookmark-test
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.db.tab :as tab-db]
    [shinsetsu.db.bookmark :as bookmark-db]
    [shinsetsu.db.tag :as tag-db]
    [shinsetsu.schema :as s]
    [malli.core :as m]
    [malli.error :as em]
    [malli.error :as me]
    [taoensso.timbre :as log])
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

(defexpect normal-create-bookmark
  (let [bookmark-title "hello"
        bookmark-url   "world"
        bookmark       (bookmark-db/create-bookmark {:bookmark/title   bookmark-title
                                                     :bookmark/url     bookmark-url
                                                     :bookmark/tab-id  @tab1-id
                                                     :bookmark/user-id @user-id})]
    (expect uuid? (:bookmark/id bookmark))
    (expect bookmark-title (:bookmark/title bookmark))
    (expect bookmark-url (:bookmark/url bookmark))
    (expect inst? (:bookmark/created bookmark))
    (expect inst? (:bookmark/updated bookmark))
    (expect @tab1-id (:bookmark/tab-id bookmark))
    (expect @user-id (:bookmark/user-id bookmark))))

(defexpect fail-create-bookmark-without-title
  (try
    (bookmark-db/create-bookmark {:bookmark/url     "foo"
                                  :bookmark/tab-id  @tab1-id
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
                                  :bookmark/tab-id  @tab1-id
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
                                  :bookmark/tab-id  @tab1-id
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
                                  :bookmark/tab-id  @tab1-id
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
                                  :bookmark/tab-id @tab1-id})
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

(defexpect normal-patch-bookmark-with-new-title-and-url-and-tab
  (let [bookmark-title     "hello"
        bookmark-url       "world"
        new-bookmark-title "foo"
        new-bookmark-url   "bar"
        bookmark           (bookmark-db/create-bookmark {:bookmark/title   bookmark-title
                                                         :bookmark/url     bookmark-url
                                                         :bookmark/tab-id  @tab1-id
                                                         :bookmark/user-id @user-id})
        bookmark-id        (:bookmark/id bookmark)
        patched-bookmark   (bookmark-db/patch-bookmark {:bookmark/id      bookmark-id
                                                        :bookmark/user-id @user-id
                                                        :bookmark/title   new-bookmark-title
                                                        :bookmark/url     new-bookmark-url
                                                        :bookmark/tab-id  @tab2-id})]
    (expect uuid? (:bookmark/id patched-bookmark))
    (expect new-bookmark-title (:bookmark/title patched-bookmark))
    (expect new-bookmark-url (:bookmark/url patched-bookmark))
    (expect inst? (:bookmark/created patched-bookmark))
    (expect inst? (:bookmark/updated patched-bookmark))
    (expect #(.after % (:bookmark/updated bookmark)) (:bookmark/updated patched-bookmark))
    (expect @tab2-id (:bookmark/tab-id patched-bookmark))
    (expect @user-id (:bookmark/user-id patched-bookmark))))

(defexpect normal-patch-bookmark-with-new-title
  (let [bookmark-title     "hello"
        bookmark-url       "world"
        new-bookmark-title "foo"
        bookmark           (bookmark-db/create-bookmark {:bookmark/title   bookmark-title
                                                         :bookmark/url     bookmark-url
                                                         :bookmark/tab-id  @tab1-id
                                                         :bookmark/user-id @user-id})
        bookmark-id        (:bookmark/id bookmark)
        patched-bookmark   (bookmark-db/patch-bookmark {:bookmark/id      bookmark-id
                                                        :bookmark/user-id @user-id
                                                        :bookmark/title   new-bookmark-title})]
    (expect uuid? (:bookmark/id patched-bookmark))
    (expect new-bookmark-title (:bookmark/title patched-bookmark))
    (expect bookmark-url (:bookmark/url patched-bookmark))
    (expect inst? (:bookmark/created patched-bookmark))
    (expect inst? (:bookmark/updated patched-bookmark))
    (expect #(.after % (:bookmark/updated bookmark)) (:bookmark/updated patched-bookmark))
    (expect @tab1-id (:bookmark/tab-id patched-bookmark))
    (expect @user-id (:bookmark/user-id patched-bookmark))))

(defexpect normal-patch-bookmark-with-new-url
  (let [bookmark-title   "hello"
        bookmark-url     "world"
        new-bookmark-url "foo"
        bookmark         (bookmark-db/create-bookmark {:bookmark/title   bookmark-title
                                                       :bookmark/url     bookmark-url
                                                       :bookmark/tab-id  @tab1-id
                                                       :bookmark/user-id @user-id})
        bookmark-id      (:bookmark/id bookmark)
        patched-bookmark (bookmark-db/patch-bookmark {:bookmark/id      bookmark-id
                                                      :bookmark/user-id @user-id
                                                      :bookmark/url     new-bookmark-url})]
    (expect uuid? (:bookmark/id patched-bookmark))
    (expect bookmark-title (:bookmark/title patched-bookmark))
    (expect new-bookmark-url (:bookmark/url patched-bookmark))
    (expect inst? (:bookmark/created patched-bookmark))
    (expect inst? (:bookmark/updated patched-bookmark))
    (expect #(.after % (:bookmark/updated bookmark)) (:bookmark/updated patched-bookmark))
    (expect @tab1-id (:bookmark/tab-id patched-bookmark))
    (expect @user-id (:bookmark/user-id patched-bookmark))))

(defexpect normal-patch-bookmark-with-new-tab
  (let [bookmark-title   "hello"
        bookmark-url     "world"
        bookmark         (bookmark-db/create-bookmark {:bookmark/title   bookmark-title
                                                       :bookmark/url     bookmark-url
                                                       :bookmark/tab-id  @tab1-id
                                                       :bookmark/user-id @user-id})
        bookmark-id      (:bookmark/id bookmark)
        patched-bookmark (bookmark-db/patch-bookmark {:bookmark/id      bookmark-id
                                                      :bookmark/user-id @user-id
                                                      :bookmark/tab-id  @tab2-id})]
    (expect uuid? (:bookmark/id patched-bookmark))
    (expect bookmark-title (:bookmark/title patched-bookmark))
    (expect bookmark-url (:bookmark/url patched-bookmark))
    (expect inst? (:bookmark/created patched-bookmark))
    (expect inst? (:bookmark/updated patched-bookmark))
    (expect #(.after % (:bookmark/updated bookmark)) (:bookmark/updated patched-bookmark))
    (expect @tab2-id (:bookmark/tab-id patched-bookmark))
    (expect @user-id (:bookmark/user-id patched-bookmark))))

(defexpect fail-patch-bookmark-with-invalid-title
  (try
    (let [bookmark-title     "hello"
          bookmark-url       "world"
          new-bookmark-title ""
          bookmark           (bookmark-db/create-bookmark {:bookmark/title   bookmark-title
                                                           :bookmark/url     bookmark-url
                                                           :bookmark/tab-id  @tab1-id
                                                           :bookmark/user-id @user-id})
          bookmark-id        (:bookmark/id bookmark)]
      (bookmark-db/patch-bookmark {:bookmark/id      bookmark-id
                                   :bookmark/user-id @user-id
                                   :bookmark/title   new-bookmark-title})
      (expect false))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid bookmark" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/title ["should be at least 1 characters"]}} data)))))

(defexpect fail-patch-bookmark-with-invalid-url
  (try
    (let [bookmark-title   "foo"
          bookmark-url     "bar"
          new-bookmark-url ""
          bookmark         (bookmark-db/create-bookmark {:bookmark/title   bookmark-title
                                                         :bookmark/url     bookmark-url
                                                         :bookmark/tab-id  @tab1-id
                                                         :bookmark/user-id @user-id})
          bookmark-id      (:bookmark/id bookmark)]
      (bookmark-db/patch-bookmark {:bookmark/id      bookmark-id
                                   :bookmark/user-id @user-id
                                   :bookmark/url     new-bookmark-url})
      (expect false))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid bookmark" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/url ["should be at least 1 characters"]}} data)))))

(defexpect fail-patch-bookmark-with-invalid-tab
  (try
    (let [bookmark-title "foo"
          bookmark-url   "bar"
          new-tab-id     "boo"
          bookmark       (bookmark-db/create-bookmark {:bookmark/title   bookmark-title
                                                       :bookmark/url     bookmark-url
                                                       :bookmark/tab-id  @tab1-id
                                                       :bookmark/user-id @user-id})
          bookmark-id    (:bookmark/id bookmark)]
      (bookmark-db/patch-bookmark {:bookmark/id      bookmark-id
                                   :bookmark/user-id @user-id
                                   :bookmark/tab-id  new-tab-id})
      (expect false))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid bookmark" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/tab-id ["should be a uuid"]}} data)))))

(defexpect fail-patch-bookmark-with-nonexistent-tab
  (try
    (let [bookmark-title "foo"
          bookmark-url   "bar"
          new-tab-id     (UUID/randomUUID)
          bookmark       (bookmark-db/create-bookmark {:bookmark/title   bookmark-title
                                                       :bookmark/url     bookmark-url
                                                       :bookmark/tab-id  @tab1-id
                                                       :bookmark/user-id @user-id})
          bookmark-id    (:bookmark/id bookmark)]
      (bookmark-db/patch-bookmark {:bookmark/id      bookmark-id
                                   :bookmark/user-id @user-id
                                   :bookmark/tab-id  new-tab-id})
      (expect false))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Nonexistent tab" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/tab-id ["nonexistent"]}} data)))))

(defexpect normal-delete-bookmark
  (let [bookmark         (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                       :bookmark/url     "bar"
                                                       :bookmark/tab-id  @tab1-id
                                                       :bookmark/user-id @user-id})
        bookmark-id      (:bookmark/id bookmark)
        deleted-bookmark (bookmark-db/delete-bookmark {:bookmark/id bookmark-id :bookmark/user-id @user-id})
        fetched-bookmark (bookmark-db/fetch-bookmark {:bookmark/id bookmark-id :user/id @user-id})]
    (expect nil fetched-bookmark)
    (expect (:bookmark/id bookmark) (:bookmark/id deleted-bookmark))
    (expect (:bookmark/title bookmark) (:bookmark/title deleted-bookmark))
    (expect (:bookmark/url bookmark) (:bookmark/url deleted-bookmark))
    (expect (:bookmark/created bookmark) (:bookmark/created deleted-bookmark))
    (expect (:bookmark/updated bookmark) (:bookmark/updated deleted-bookmark))))

(defexpect normal-delete-nonexistent-bookmark
  (let [deleted-bookmark (bookmark-db/delete-bookmark {:bookmark/id (UUID/randomUUID) :bookmark/user-id @user-id})]
    (expect nil deleted-bookmark)))

(defexpect fail-delete-bookmark-with-invalid-id
  (try
    (bookmark-db/delete-bookmark {:bookmark/id "foo" :bookmark/user-id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid bookmark" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/id ["should be a uuid"]}} data)))))

(comment
  (require '[kaocha.repl :as k])
  (k/run #'shinsetsu.db.bookmark-test/fail-delete-bookmark-with-invalid-id))

(defexpect normal-fetch-bookmark
  (let [bookmark         (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                       :bookmark/url     "bar"
                                                       :bookmark/tab-id  @tab1-id
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
                                                        :bookmark/tab-id  @tab1-id
                                                        :bookmark/user-id @user-id})
        bookmark2         (bookmark-db/create-bookmark {:bookmark/title   bookmark2-title
                                                        :bookmark/url     bookmark2-url
                                                        :bookmark/tab-id  @tab1-id
                                                        :bookmark/user-id @user-id})
        fetched-bookmarks (bookmark-db/fetch-bookmarks {:user/id @user-id :tab/id @tab1-id})]
    (expect [bookmark1 bookmark2] fetched-bookmarks)))

(defexpect fetch-empty-bookmarks [] (bookmark-db/fetch-bookmarks {:tab/id @tab1-id :user/id @user-id}))

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
    (bookmark-db/fetch-bookmarks {:tab/id @tab1-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid tab or user ID" message)
        (expect {:error-type :invalid-input :error-data {:user/id ["missing required key"]}} data)))))

(defexpect fail-fetch-bookmarks-with-invalid-user
  (try
    (bookmark-db/fetch-bookmarks {:tab/id @tab1-id :user/id "foo"})
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

(defexpect normal-create-bookmark-tag
  (let [bookmark     (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                   :bookmark/url     "bar"
                                                   :bookmark/user-id @user-id
                                                   :bookmark/tab-id  @tab1-id})
        bookmark-id  (:bookmark/id bookmark)
        tag          (tag-db/create-tag {:tag/name "bob" :tag/user-id @user-id})
        tag-id       (:tag/id tag)
        bookmark-tag (bookmark-db/create-bookmark-tag {:bookmark/id bookmark-id
                                                       :tag/id      tag-id
                                                       :user/id     @user-id})]
    (expect bookmark-id (:bookmark-tag/bookmark-id bookmark-tag))
    (expect tag-id (:bookmark-tag/tag-id bookmark-tag))
    (expect @user-id (:bookmark-tag/user-id bookmark-tag))))

(defexpect fail-create-bookmark-tag-with-invalid-bookmark-id
  (try
    (let [bookmark-id "foo"
          tag         (tag-db/create-tag {:tag/name "bob" :tag/user-id @user-id})
          tag-id      (:tag/id tag)]
      (bookmark-db/create-bookmark-tag {:bookmark/id bookmark-id :tag/id tag-id :user/id @user-id})
      (expect false))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid bookmark, tag or user ID" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/id ["should be a uuid"]}} data)))))

(defexpect fail-create-bookmark-tag-with-invalid-tag-id
  (try
    (let [bookmark    (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                    :bookmark/url     "bar"
                                                    :bookmark/user-id @user-id
                                                    :bookmark/tab-id  @tab1-id})
          bookmark-id (:bookmark/id bookmark)
          tag-id      "foo"]
      (bookmark-db/create-bookmark-tag {:bookmark/id bookmark-id :tag/id tag-id :user/id @user-id})
      (expect false))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid bookmark, tag or user ID" message)
        (expect {:error-type :invalid-input :error-data {:tag/id ["should be a uuid"]}} data)))))

(defexpect normal-fetch-bookmarks-by-tag
  (let [bookmark             (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                           :bookmark/url     "bar"
                                                           :bookmark/user-id @user-id
                                                           :bookmark/tab-id  @tab1-id})
        bookmark-id          (:bookmark/id bookmark)
        tag                  (tag-db/create-tag {:tag/name "bob" :tag/user-id @user-id})
        tag-id               (:tag/id tag)
        bookmark-tag         (bookmark-db/create-bookmark-tag {:bookmark/id bookmark-id
                                                               :tag/id      tag-id
                                                               :user/id     @user-id})
        fetched-bookmark-tag (bookmark-db/fetch-bookmarks-by-tag {:tag/id tag-id :user/id @user-id})]
    (expect [bookmark-tag] fetched-bookmark-tag)))

(defexpect normal-fetch-empty-bookmarks-by-tag
  (let [tag                  (tag-db/create-tag {:tag/name "bob" :tag/user-id @user-id})
        tag-id               (:tag/id tag)
        fetched-bookmark-tag (bookmark-db/fetch-bookmarks-by-tag {:tag/id tag-id :user/id @user-id})]
    (expect [] fetched-bookmark-tag)))

(defexpect normal-fetch-bookmarks-by-nonexistent-tag
  (let [fetched-bookmark-tag (bookmark-db/fetch-bookmarks-by-tag {:tag/id (UUID/randomUUID) :user/id @user-id})]
    (expect [] fetched-bookmark-tag)))

(defexpect fail-fetch-bookmarks-by-invalid-tag
  (try
    (bookmark-db/fetch-bookmarks-by-tag {:tag/id "foo" :user/id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid tag or user ID" message)
        (expect {:error-type :invalid-input :error-data {:tag/id ["should be a uuid"]}} data)))))

(defexpect normal-fetch-tags-by-bookmark
  (let [bookmark             (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                           :bookmark/url     "bar"
                                                           :bookmark/user-id @user-id
                                                           :bookmark/tab-id  @tab1-id})
        bookmark-id          (:bookmark/id bookmark)
        tag                  (tag-db/create-tag {:tag/name "bob" :tag/user-id @user-id})
        tag-id               (:tag/id tag)
        bookmark-tag         (bookmark-db/create-bookmark-tag {:bookmark/id bookmark-id
                                                               :tag/id      tag-id
                                                               :user/id     @user-id})
        fetched-bookmark-tag (bookmark-db/fetch-tags-by-bookmark {:bookmark/id bookmark-id :user/id @user-id})]
    (expect [bookmark-tag] fetched-bookmark-tag)))

(defexpect normal-fetch-empty-tags-by-bookmark
  (let [bookmark             (bookmark-db/create-bookmark {:bookmark/title   "bob"
                                                           :bookmark/url     "bar"
                                                           :bookmark/tab-id  @tab1-id
                                                           :bookmark/user-id @user-id})
        bookmark-id          (:bookmark/id bookmark)
        fetched-bookmark-tag (bookmark-db/fetch-tags-by-bookmark {:bookmark/id bookmark-id :user/id @user-id})]
    (expect [] fetched-bookmark-tag)))

(defexpect normal-fetch-tags-by-nonexistent-bookmark
  (let [fetched-bookmark-tag (bookmark-db/fetch-tags-by-bookmark {:bookmark/id (UUID/randomUUID) :user/id @user-id})]
    (expect [] fetched-bookmark-tag)))

(defexpect fail-fetch-tags-by-invalid-bookmark
  (try
    (bookmark-db/fetch-tags-by-bookmark {:bookmark/id "foo" :user/id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid bookmark or user ID" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/id ["should be a uuid"]}} data)))))


(defexpect normal-delete-bookmark-tag
  (let [bookmark             (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                           :bookmark/url     "bar"
                                                           :bookmark/user-id @user-id
                                                           :bookmark/tab-id  @tab1-id})
        bookmark-id          (:bookmark/id bookmark)
        tag                  (tag-db/create-tag {:tag/name "bob" :tag/user-id @user-id})
        tag-id               (:tag/id tag)
        bookmark-tag         (bookmark-db/create-bookmark-tag {:bookmark/id bookmark-id
                                                               :tag/id      tag-id
                                                               :user/id     @user-id})
        deleted-bookmark-tag (bookmark-db/delete-bookmark-tag {:bookmark/id bookmark-id
                                                               :tag/id      tag-id
                                                               :user/id     @user-id})
        fetched-bookmark-tag (bookmark-db/fetch-bookmarks-by-tag {:tag/id tag-id :user/id @user-id})]
    (expect [] fetched-bookmark-tag)
    (expect bookmark-tag deleted-bookmark-tag)))

(defexpect normal-delete-bookmark-tag-by-nonexistent-bookmark
  (let [bookmark-id          (UUID/randomUUID)
        tag                  (tag-db/create-tag {:tag/name "bob" :tag/user-id @user-id})
        tag-id               (:tag/id tag)
        deleted-bookmark-tag (bookmark-db/delete-bookmark-tag {:bookmark/id bookmark-id
                                                               :tag/id      tag-id
                                                               :user/id     @user-id})]
    (expect nil deleted-bookmark-tag)))

(defexpect normal-delete-bookmark-tag-by-nonexistent-tag
  (let [bookmark             (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                           :bookmark/url     "bar"
                                                           :bookmark/user-id @user-id
                                                           :bookmark/tab-id  @tab1-id})
        bookmark-id          (:bookmark/id bookmark)
        tag-id               (UUID/randomUUID)
        deleted-bookmark-tag (bookmark-db/delete-bookmark-tag {:bookmark/id bookmark-id
                                                               :tag/id      tag-id
                                                               :user/id     @user-id})]
    (expect nil deleted-bookmark-tag)))

(defexpect fail-delete-bookmark-tag-by-invalid-tag
  (try
    (bookmark-db/delete-bookmark-tag {:tag/id "foo" :bookmark/id (UUID/randomUUID) :user/id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid bookmark, tag or user ID" message)
        (expect {:error-type :invalid-input :error-data {:tag/id ["should be a uuid"]}} data)))))

(defexpect fail-delete-bookmark-tag-by-invalid-bookmark
  (try
    (bookmark-db/delete-bookmark-tag {:tag/id (UUID/randomUUID) :bookmark/id "foo" :user/id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid bookmark, tag or user ID" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/id ["should be a uuid"]}} data)))))

(comment
  (require '[kaocha.repl :as k])
  (require '[shinsetsu.db.bookmark-test])
  (k/run 'shinsetsu.db.bookmark-test)
  (k/run #'shinsetsu.db.bookmark-test/fail-fetch-tags-by-invalid-bookmark)
  )
