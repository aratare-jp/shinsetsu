(ns shinsetsu.db.tag-test
  (:require
    [clojure.test :refer :all]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.db.tag :as tag-db]
    [taoensso.timbre :as log])
  (:import [clojure.lang ExceptionInfo]))

(def user (atom nil))
(def user-id (atom nil))

(defn user-setup
  [f]
  (reset! user (user-db/create-user {:user/username "foo" :user/password "bar"}))
  (reset! user-id (:user/id @user))
  (f))

(use-fixtures :once db-setup)
(use-fixtures :each db-cleanup user-setup)

(defexpect ^:db ^:unit ^:tag normal-create-tag
  (let [tag-name   "hello"
        tag-colour "#ffffff"
        tag        (tag-db/create-tag {:tag/name    tag-name
                                       :tag/colour  tag-colour
                                       :tag/user-id @user-id})]
    (expect uuid? (:tag/id tag))
    (expect tag-name (:tag/name tag))
    (expect tag-colour (:tag/colour tag))
    (expect inst? (:tag/created tag))
    (expect inst? (:tag/updated tag))
    (expect @user-id (:tag/user-id tag))))

(defexpect ^:db ^:unit ^:tag normal-create-tag-without-colour
  (let [tag-name "hello"
        tag      (tag-db/create-tag {:tag/name tag-name :tag/user-id @user-id})]
    (expect uuid? (:tag/id tag))
    (expect tag-name (:tag/name tag))
    (expect inst? (:tag/created tag))
    (expect inst? (:tag/updated tag))
    (expect @user-id (:tag/user-id tag))))

(defexpect ^:db ^:unit ^:tag fail-create-tag-without-name
  (try
    (tag-db/create-tag {:tag/user-id (random-uuid)})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tag/name ["missing required key"]}} data)))))

(defexpect ^:db ^:unit ^:tag fail-create-tag-with-invalid-name
  (try
    (tag-db/create-tag {:tag/name "" :tag/user-id @user-id})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tag/name ["should be at least 1 characters"]}} data)))))

(defexpect ^:db ^:unit ^:tag fail-create-tag-without-user
  (try
    (tag-db/create-tag {:tag/name "foo"})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tag/user-id ["missing required key"]}} data)))))

(defexpect ^:db ^:unit ^:tag fail-create-tag-with-invalid-user
  (try
    (tag-db/create-tag {:tag/user-id "foo" :tag/name "foo"})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tag/user-id ["should be a uuid"]}} data)))))

(defexpect ^:db ^:unit ^:tag normal-patch-tag-with-new-name-and-colour
  (let [tag            (tag-db/create-tag {:tag/name "foo" :tag/colour "#ffffff" :tag/user-id @user-id})
        new-tag-name   "hello"
        new-tag-colour "#000000"
        tag-id         (:tag/id tag)
        patched-tag    (tag-db/patch-tag {:tag/id      tag-id
                                          :tag/name    new-tag-name
                                          :tag/colour  new-tag-colour
                                          :tag/user-id @user-id})]
    (expect (:tag/id tag) (:tag/id patched-tag))
    (expect new-tag-name (:tag/name patched-tag))
    (expect new-tag-colour (:tag/colour patched-tag))
    (expect (:tag/created tag) (:tag/created patched-tag))
    (expect #(.after % (:tag/updated tag)) (:tag/updated patched-tag))
    (expect @user-id (:tag/user-id patched-tag))))

(defexpect ^:db ^:unit ^:tag normal-patch-tag-with-new-name
  (let [tag          (tag-db/create-tag {:tag/name "foo" :tag/colour "#ffffff" :tag/user-id @user-id})
        new-tag-name "hello"
        tag-id       (:tag/id tag)
        patched-tag  (tag-db/patch-tag {:tag/id      tag-id
                                        :tag/name    new-tag-name
                                        :tag/user-id @user-id})]
    (expect (:tag/id tag) (:tag/id patched-tag))
    (expect new-tag-name (:tag/name patched-tag))
    (expect (:tag/colour tag) (:tag/colour patched-tag))
    (expect (:tag/created tag) (:tag/created patched-tag))
    (expect #(.after % (:tag/updated tag)) (:tag/updated patched-tag))
    (expect @user-id (:tag/user-id patched-tag))))

(defexpect ^:db ^:unit ^:tag normal-patch-tag-with-new-colour
  (let [tag            (tag-db/create-tag {:tag/name "foo" :tag/colour "#ffffff" :tag/user-id @user-id})
        new-tag-colour "#000000"
        tag-id         (:tag/id tag)
        patched-tag    (tag-db/patch-tag {:tag/id      tag-id
                                          :tag/colour  new-tag-colour
                                          :tag/user-id @user-id})]
    (expect (:tag/id tag) (:tag/id patched-tag))
    (expect (:tag/name tag) (:tag/name patched-tag))
    (expect new-tag-colour (:tag/colour patched-tag))
    (expect (:tag/created tag) (:tag/created patched-tag))
    (expect #(.after % (:tag/updated tag)) (:tag/updated patched-tag))
    (expect @user-id (:tag/user-id patched-tag))))

(defexpect ^:db ^:unit ^:tag normal-patch-tag-without-new-name-and-colour
  (let [tag         (tag-db/create-tag {:tag/name "foo" :tag/colour "#ffffff" :tag/user-id @user-id})
        tag-id      (:tag/id tag)
        patched-tag (tag-db/patch-tag {:tag/id tag-id :tag/user-id @user-id})]
    (expect (:tag/id tag) (:tag/id patched-tag))
    (expect (:tag/name tag) (:tag/name patched-tag))
    (expect (:tag/colour tag) (:tag/colour patched-tag))
    (expect (:tag/created tag) (:tag/created patched-tag))
    (expect #(.after % (:tag/updated tag)) (:tag/updated patched-tag))
    (expect @user-id (:tag/user-id patched-tag))))

(defexpect ^:db ^:unit ^:tag fail-patch-tag-with-invalid-name
  (try
    (let [tag    (tag-db/create-tag {:tag/name "foo" :tag/colour "#ffffff" :tag/user-id @user-id})
          tag-id (:tag/id tag)]
      (tag-db/patch-tag {:tag/id tag-id :tag/user-id @user-id :tag/name ""})
      (expect false))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tag/name ["should be at least 1 characters"]}} data)))))

(defexpect ^:db ^:unit ^:tag fail-patch-tag-with-invalid-colour
  (try
    (let [tag    (tag-db/create-tag {:tag/name "foo" :tag/colour "#ffffff" :tag/user-id @user-id})
          tag-id (:tag/id tag)]
      (tag-db/patch-tag {:tag/id tag-id :tag/user-id @user-id :tag/colour "foo"})
      (expect false))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tag/colour ["must have hex colour format"]}} data)))))

(defexpect ^:db ^:unit ^:tag normal-delete-tag
  (let [tag         (tag-db/create-tag {:tag/name "foo" :tag/colour "#ffffff" :tag/user-id @user-id})
        tag-id      (:tag/id tag)
        deleted-tag (tag-db/delete-tag {:tag/id tag-id :tag/user-id @user-id})]
    (expect tag deleted-tag)
    (expect nil (tag-db/fetch-tag #:tag{:id tag-id :user-id @user-id}))))

(defexpect ^:db ^:unit ^:tag normal-delete-tag-with-nonexistent-id
  (let [tag-id      (random-uuid)
        deleted-tag (tag-db/delete-tag {:tag/id tag-id :tag/user-id @user-id})]
    (expect nil deleted-tag)))

(defexpect ^:db ^:unit ^:tag normal-delete-tag-with-nonexistent-user-id
  (let [tag         (tag-db/create-tag {:tag/name "foo" :tag/colour "#ffffff" :tag/user-id @user-id})
        tag-id      (:tag/id tag)
        user-id     (random-uuid)
        deleted-tag (tag-db/delete-tag {:tag/id tag-id :tag/user-id user-id})]
    (expect nil deleted-tag)))

(defexpect ^:db ^:unit ^:tag fail-delete-tag-without-tag-id
  (try
    (tag-db/delete-tag {:tag/user-id @user-id})
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tag/id ["missing required key"]}} data)))))

(defexpect ^:db ^:unit ^:tag fail-delete-tag-with-invalid-tag-id
  (try
    (tag-db/delete-tag {:tag/id "" :tag/user-id @user-id})
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tag/id ["should be a uuid"]}} data)))))

(defexpect ^:db ^:unit ^:tag fail-delete-tag-without-user-id
  (try
    (let [tag    (tag-db/create-tag {:tag/name "foo" :tag/colour "#ffffff" :tag/user-id @user-id})
          tag-id (:tag/id tag)]
      (tag-db/delete-tag {:tag/id tag-id}))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tag/user-id ["missing required key"]}} data)))))

(defexpect ^:db ^:unit ^:tag fail-delete-tag-with-invalid-user-id
  (try
    (let [tag    (tag-db/create-tag {:tag/name "foo" :tag/colour "#ffffff" :tag/user-id @user-id})
          tag-id (:tag/id tag)]
      (tag-db/delete-tag {:tag/id tag-id :tag/user-id ""}))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tag/user-id ["should be a uuid"]}} data)))))

(defexpect ^:db ^:unit ^:tag normal-fetch-tags
  (let [tag1-name    "foo"
        tag2-name    "bar"
        tag1         (tag-db/create-tag #:tag{:name tag1-name :user-id @user-id})
        tag2         (tag-db/create-tag #:tag{:name tag2-name :user-id @user-id})
        fetched-tags (tag-db/fetch-tags #:tag{:user-id @user-id})]
    (expect [tag1 tag2] fetched-tags)))

(defexpect ^:db ^:unit ^:tag normal-fetch-tags-with-name-between
  (let [tag1-name "food"
        tag2-name "goof"
        tag3-name "bar"
        tag1      (tag-db/create-tag #:tag{:name tag1-name :user-id @user-id})
        tag2      (tag-db/create-tag #:tag{:name tag2-name :user-id @user-id})
        tag3      (tag-db/create-tag #:tag{:name tag3-name :user-id @user-id})
        actual1   (tag-db/fetch-tags #:tag{:user-id @user-id :name "oo"})
        actual2   (tag-db/fetch-tags #:tag{:user-id @user-id :name "oo" :name-pos :between})]
    (expect [tag1 tag2] actual1)
    (expect [tag1 tag2] actual2)))

(defexpect ^:db ^:unit ^:tag normal-fetch-tags-with-name-start
  (let [tag1-name    "foo"
        tag2-name    "fim"
        tag3-name    "bar"
        tag1         (tag-db/create-tag #:tag{:name tag1-name :user-id @user-id})
        tag2         (tag-db/create-tag #:tag{:name tag2-name :user-id @user-id})
        tag3         (tag-db/create-tag #:tag{:name tag3-name :user-id @user-id})
        fetched-tags (tag-db/fetch-tags #:tag{:user-id @user-id :name "f" :name-pos :start})]
    (expect [tag1 tag2] fetched-tags)))

(defexpect ^:db ^:unit ^:tag normal-fetch-tags-with-name-end
  (let [tag1-name    "food"
        tag2-name    "good"
        tag3-name    "bar"
        tag1         (tag-db/create-tag #:tag{:name tag1-name :user-id @user-id})
        tag2         (tag-db/create-tag #:tag{:name tag2-name :user-id @user-id})
        tag3         (tag-db/create-tag #:tag{:name tag3-name :user-id @user-id})
        fetched-tags (tag-db/fetch-tags #:tag{:user-id @user-id :name "ood" :name-pos :end})]
    (expect [tag1 tag2] fetched-tags)))

(defexpect ^:db ^:unit ^:tag normal-fetch-empty-tags [] (tag-db/fetch-tags #:tag{:user-id @user-id}))
(defexpect ^:db ^:unit ^:tag normal-fetch-tags-from-nonexistent-user [] (tag-db/fetch-tags #:tag{:user-id (random-uuid)}))

(defexpect ^:db ^:unit ^:tag fail-fetch-tags-without-user
  (try
    (tag-db/fetch-tags {})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tag/user-id ["missing required key"]}} data)))))

(defexpect ^:db ^:unit ^:tag fail-fetch-tags-with-invalid-user
  (try
    (tag-db/fetch-tags #:tag{:user-id "boo"})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tag/user-id ["should be a uuid"]}} data)))))

(defexpect ^:db ^:unit ^:tag normal-fetch-tag
  (let [tag-name    "hello"
        tag-colour  "#ffffff"
        tag         (tag-db/create-tag {:tag/name tag-name :tag/colour tag-colour :tag/user-id @user-id})
        tag-id      (:tag/id tag)
        fetched-tag (tag-db/fetch-tag #:tag{:id tag-id :user-id @user-id})]
    (expect tag fetched-tag)))

(defexpect ^:db ^:unit ^:tag normal-fetch-nonexistent-tag
  (let [fetched-tag (tag-db/fetch-tag #:tag{:id (random-uuid) :user-id @user-id})]
    (expect nil fetched-tag)))

(defexpect ^:db ^:unit ^:tag fail-fetch-tag-without-id-and-tag-id
  (try
    (tag-db/fetch-tag {})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid input" message)
        (expect
          {:error-type :invalid-input
           :error-data #:tag{:id      ["missing required key"]
                             :user-id ["missing required key"]}}
          data)))))

(defexpect ^:db ^:unit ^:tag fail-fetch-tag-without-id
  (try
    (tag-db/fetch-tag {:tag/user-id (random-uuid)})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tag/id ["missing required key"]}} data)))))

(defexpect ^:db ^:unit ^:tag fail-fetch-tag-with-invalid-id
  (try
    (tag-db/fetch-tag {:tag/id "foo" :tag/user-id (random-uuid)})
    (expect false)
    (catch ExceptionInfo e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tag/id ["should be a uuid"]}} data)))))

(defexpect ^:db ^:unit ^:tag fail-fetch-tag-without-user
  (try
    (tag-db/fetch-tag {:tag/id (random-uuid)})
    (expect false)
    (catch Exception e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tag/user-id ["missing required key"]}} data)))))

(defexpect ^:db ^:unit ^:tag fail-fetch-tag-with-invalid-user
  (try
    (tag-db/fetch-tag #:tag{:id (random-uuid) :user-id "not real"})
    (expect false)
    (catch ExceptionInfo e
      (let [message (ex-message e)
            data    (ex-data e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:tag/user-id ["should be a uuid"]}} data)))))

(comment
  (require '[kaocha.repl :as k])
  (k/run #'shinsetsu.db.tag-test/normal-fetch-tag)
  (k/run 'shinsetsu.db.tag-test))
