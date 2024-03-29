(ns shinsetsu.db.bookmark-tag-test
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.db.bookmark :as bdb]
    [shinsetsu.db.tag :as tgdb]
    [shinsetsu.db.bookmark-tag :as btdb]
    [shinsetsu.db.user :as udb]
    [shinsetsu.db.tab :as tdb]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]))

(def user (atom nil))
(def user-id (atom nil))
(def tab1 (atom nil))
(def tab1-id (atom nil))
(def tab2 (atom nil))
(def tab2-id (atom nil))

(defn user-tab-setup
  [f]
  (reset! user (udb/create-user {:user/username "foo" :user/password "bar"}))
  (reset! user-id (:user/id @user))
  (reset! tab1 (tdb/create-tab {:tab/name "foo" :tab/user-id @user-id}))
  (reset! tab1-id (:tab/id @tab1))
  (reset! tab2 (tdb/create-tab {:tab/name "baz" :tab/user-id @user-id}))
  (reset! tab2-id (:tab/id @tab2))
  (f))

(use-fixtures :once db-setup)
(use-fixtures :each db-cleanup user-tab-setup)

(defexpect ^:bookmark-tag ^:db ^:unit normal-create-bookmark-tag
  (let [bookmark     (bdb/create-bookmark #:bookmark{:title "foo" :url "bar" :user-id @user-id :tab-id @tab1-id})
        bookmark-id  (:bookmark/id bookmark)
        tag          (tgdb/create-tag {:tag/name "bob" :tag/user-id @user-id})
        tag-id       (:tag/id tag)
        bookmark-tag (btdb/create-bookmark-tag #:bookmark-tag{:bookmark-id bookmark-id :tag-id tag-id :user-id @user-id})]
    (expect bookmark-id (:bookmark-tag/bookmark-id bookmark-tag))
    (expect tag-id (:bookmark-tag/tag-id bookmark-tag))
    (expect @user-id (:bookmark-tag/user-id bookmark-tag))))

(defexpect ^:bookmark-tag ^:db ^:unit fail-create-bookmark-tag-with-invalid-bookmark-id
  (try
    (let [bookmark-id "foo"
          tag         (tgdb/create-tag {:tag/name "bob" :tag/user-id @user-id})
          tag-id      (:tag/id tag)]
      (btdb/create-bookmark-tag #:bookmark-tag{:bookmark-id bookmark-id :tag-id tag-id :user-id @user-id})
      (expect false))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark-tag/bookmark-id ["should be a uuid"]}} data)))))

(defexpect ^:bookmark-tag ^:db ^:unit fail-create-bookmark-tag-with-invalid-tag-id
  (try
    (let [bookmark    (bdb/create-bookmark #:bookmark{:title "foo" :url "bar" :user-id @user-id :tab-id @tab1-id})
          bookmark-id (:bookmark/id bookmark)
          tag-id      "foo"]
      (btdb/create-bookmark-tag #:bookmark-tag{:bookmark-id bookmark-id :tag-id tag-id :user-id @user-id})
      (expect false))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark-tag/tag-id ["should be a uuid"]}} data)))))

(defexpect ^:bookmark-tag ^:db ^:unit normal-fetch-tags-by-bookmark
  (let [bookmark    (bdb/create-bookmark #:bookmark{:title "foo" :url "bar" :user-id @user-id :tab-id @tab1-id})
        bookmark-id (:bookmark/id bookmark)
        tag         (tgdb/create-tag {:tag/name "bob" :tag/user-id @user-id})
        tag-id      (:tag/id tag)
        expected    (btdb/create-bookmark-tag #:bookmark-tag{:bookmark-id bookmark-id :tag-id tag-id :user-id @user-id})
        actual      (btdb/fetch-tags-by-bookmark #:bookmark{:id bookmark-id :user-id @user-id})]
    (expect [expected] actual)))

(defexpect ^:bookmark-tag ^:db ^:unit normal-fetch-empty-tags-by-bookmark
  (let [bookmark    (bdb/create-bookmark #:bookmark{:title "bob" :url "bar" :tab-id @tab1-id :user-id @user-id})
        bookmark-id (:bookmark/id bookmark)
        actual      (btdb/fetch-tags-by-bookmark #:bookmark{:id bookmark-id :user-id @user-id})]
    (expect [] actual)))

(defexpect ^:bookmark-tag ^:db ^:unit normal-fetch-tags-by-nonexistent-bookmark
  (let [actual (btdb/fetch-tags-by-bookmark #:bookmark{:id (random-uuid) :user-id @user-id})]
    (expect [] actual)))

(defexpect ^:bookmark-tag ^:db ^:unit fail-fetch-tags-by-invalid-bookmark
  (try
    (btdb/fetch-tags-by-bookmark #:bookmark{:id "foo" :user-id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/id ["should be a uuid"]}} data)))))

(defexpect ^:bookmark-tag ^:db ^:unit normal-delete-bookmark-tag
  (let [bookmark    (bdb/create-bookmark #:bookmark{:title "foo" :url "bar" :user-id @user-id :tab-id @tab1-id})
        bookmark-id (:bookmark/id bookmark)
        tag         (tgdb/create-tag {:tag/name "bob" :tag/user-id @user-id})
        tag-id      (:tag/id tag)
        bt          (btdb/create-bookmark-tag #:bookmark-tag{:bookmark-id bookmark-id :tag-id tag-id :user-id @user-id})
        deleted-bt  (btdb/delete-bookmark-tag #:bookmark-tag{:bookmark-id bookmark-id :tag-id tag-id :user-id @user-id})
        fetched-bt  (btdb/fetch-tags-by-bookmark #:bookmark{:id bookmark-id :user-id @user-id})]
    (expect [] fetched-bt)
    (expect bt deleted-bt)))

(defexpect ^:bookmark-tag ^:db ^:unit normal-delete-bookmark-tag-by-nonexistent-bookmark
  (let [bookmark-id (random-uuid)
        tag         (tgdb/create-tag {:tag/name "bob" :tag/user-id @user-id})
        tag-id      (:tag/id tag)
        actual      (btdb/delete-bookmark-tag #:bookmark-tag{:bookmark-id bookmark-id :tag-id tag-id :user-id @user-id})]
    (expect nil actual)))

(defexpect ^:bookmark-tag ^:db ^:unit normal-delete-bookmark-tag-by-nonexistent-tag
  (let [bookmark    (bdb/create-bookmark #:bookmark{:title "foo" :url "bar" :user-id @user-id :tab-id @tab1-id})
        bookmark-id (:bookmark/id bookmark)
        tag-id      (random-uuid)
        actual      (btdb/delete-bookmark-tag #:bookmark-tag{:bookmark-id bookmark-id :tag-id tag-id :user-id @user-id})]
    (expect nil actual)))

(defexpect ^:bookmark-tag ^:db ^:unit fail-delete-bookmark-tag-by-invalid-tag
  (try
    (btdb/delete-bookmark-tag #:bookmark-tag{:tag-id "foo" :bookmark-id (random-uuid) :user-id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark-tag/tag-id ["should be a uuid"]}} data)))))

(defexpect ^:bookmark-tag ^:db ^:unit fail-delete-bookmark-tag-by-invalid-bookmark
  (try
    (btdb/delete-bookmark-tag #:bookmark-tag{:tag-id (random-uuid) :bookmark-id "foo" :user-id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark-tag/bookmark-id ["should be a uuid"]}} data)))))

(defexpect ^:bookmark-tag ^:db ^:unit normal-delete-bookmark-tags
  (let [bookmark    (bdb/create-bookmark #:bookmark{:title "foo" :url "bar" :user-id @user-id :tab-id @tab1-id})
        bookmark-id (:bookmark/id bookmark)
        tag1        (tgdb/create-tag {:tag/name "bob" :tag/user-id @user-id})
        tag1-id     (:tag/id tag1)
        tag2        (tgdb/create-tag {:tag/name "alice" :tag/user-id @user-id})
        tag2-id     (:tag/id tag2)
        bt1         (btdb/create-bookmark-tag #:bookmark-tag{:bookmark-id bookmark-id :tag-id tag1-id :user-id @user-id})
        bt2         (btdb/create-bookmark-tag #:bookmark-tag{:bookmark-id bookmark-id :tag-id tag2-id :user-id @user-id})
        deleted-bts (btdb/delete-bookmark-tags #:bookmark-tag{:bookmark-id bookmark-id :tag-ids [tag1-id tag2-id] :user-id @user-id})
        fetched-bts (btdb/fetch-tags-by-bookmark #:bookmark{:id bookmark-id :user-id @user-id})]
    (expect [] fetched-bts)
    (expect (set [bt1 bt2]) (set deleted-bts))))

(comment
  (require '[kaocha.repl :as k])
  (k/run #'shinsetsu.db.bookmark-tag-test/normal-delete-bookmark-tags))

(defexpect ^:bookmark-tag ^:db ^:unit normal-delete-empty-bookmark-tags
  (let [bookmark    (bdb/create-bookmark #:bookmark{:title "foo" :url "bar" :user-id @user-id :tab-id @tab1-id})
        bookmark-id (:bookmark/id bookmark)
        deleted-bt  (btdb/delete-bookmark-tags #:bookmark-tag{:bookmark-id bookmark-id :tag-ids [(random-uuid) (random-uuid)] :user-id @user-id})
        fetched-bt  (btdb/fetch-tags-by-bookmark #:bookmark{:id bookmark-id :user-id @user-id})]
    (expect [] fetched-bt)
    (expect [] deleted-bt)))

(comment
  (require '[kaocha.repl :as k])
  (k/run #'shinsetsu.db.bookmark-tag-test/normal-delete-empty-bookmark-tags)
  (k/run 'shinsetsu.db.bookmark-tag-test))
