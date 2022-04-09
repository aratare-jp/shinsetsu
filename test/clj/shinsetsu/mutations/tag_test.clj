(ns shinsetsu.mutations.tag-test
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [shinsetsu.parser :refer [protected-parser]]
    [shinsetsu.mutations.tag :as tag-mut]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.db.tag :as tag-db]
    [taoensso.timbre :as log]
    [buddy.hashers :as hashers])
  (:import [java.util UUID]))

(def user-id (atom nil))

(defn user-setup
  [f]
  (let [username "john"
        password "smith"
        user     (user-db/create-user {:user/username username :user/password password})]
    (reset! user-id (:user/id user))
    (f)))

(use-fixtures :once db-setup)
(use-fixtures :each db-cleanup user-setup)

(def tag-join [:tag/id :tag/name :tag/colour :tag/created :tag/updated])
(defn trim-tag [tag] (-> tag (dissoc :tag/user-id)))

(defexpect ^:mutation ^:integration ^:tag normal-create-tag
  (let [query    [{`(tag-mut/create-tag {:tag/name "foo" :tag/colour "#ffffff"}) tag-join}]
        result   (protected-parser {:request {:user/id @user-id}} query)
        actual   (get result `tag-mut/create-tag)
        tag-id   (:tag/id actual)
        expected (-> #:tag{:id tag-id :user-id @user-id} tag-db/fetch-tag trim-tag)]
    (expect expected actual)))

(defexpect ^:mutation ^:integration ^:tag normal-create-tag-without-colour
  (let [query    [{`(tag-mut/create-tag {:tag/name "foo"}) tag-join}]
        result   (protected-parser {:request {:user/id @user-id}} query)
        actual   (get result `tag-mut/create-tag)
        tag-id   (:tag/id actual)
        expected (-> #:tag{:id tag-id :user-id @user-id} tag-db/fetch-tag trim-tag)]
    (expect expected actual)))

(defexpect ^:mutation ^:integration ^:tag fail-create-tag-without-name
  (let [query    [{`(tag-mut/create-tag {}) tag-join}]
        result   (protected-parser {:request {:user/id @user-id}} query)
        actual   result
        expected {`tag-mut/create-tag {:error         true
                                       :error-message "Invalid input"
                                       :error-type    :invalid-input
                                       :error-data    {:tag/name ["missing required key"]}}}]
    (expect expected actual)))

(defexpect ^:mutation ^:integration ^:tag fail-create-tag-with-invalid-name
  (let [query    [{`(tag-mut/create-tag {:tag/name ""}) tag-join}]
        result   (protected-parser {:request {:user/id @user-id}} query)
        actual   result
        expected {`tag-mut/create-tag {:error         true
                                       :error-message "Invalid input"
                                       :error-type    :invalid-input
                                       :error-data    {:tag/name ["should be at least 1 characters"]}}}]
    (expect expected actual)))

(defexpect ^:mutation ^:integration ^:tag fail-create-tag-with-invalid-colour
  (let [query    [{`(tag-mut/create-tag {:tag/name "foo" :tag/colour "foo"}) tag-join}]
        result   (protected-parser {:request {:user/id @user-id}} query)
        actual   result
        expected {`tag-mut/create-tag {:error         true
                                       :error-message "Invalid input" :error-type :invalid-input
                                       :error-data    {:tag/colour ["must have hex colour format"]}}}]
    (expect expected actual))
  (let [query    [{`(tag-mut/create-tag {:tag/name "foo" :tag/colour ""}) tag-join}]
        result   (protected-parser {:request {:user/id @user-id}} query)
        actual   result
        expected {`tag-mut/create-tag {:error         true
                                       :error-message "Invalid input" :error-type :invalid-input
                                       :error-data    {:tag/colour ["should be at least 1 characters"
                                                                    "must have hex colour format"]}}}]
    (expect expected actual)))

(defexpect ^:mutation ^:integration ^:tag normal-patch-tag-with-new-name-and-colour
  (let [tag            (tag-db/create-tag {:tag/name "foo" :tag/colour "#ffffff" :tag/user-id @user-id})
        tag-id         (:tag/id tag)
        new-tag-name   "fim"
        new-tag-colour "#000000"
        query          [{`(tag-mut/patch-tag {:tag/id     ~tag-id
                                              :tag/name   ~new-tag-name
                                              :tag/colour ~new-tag-colour}) tag-join}]
        result         (protected-parser {:request {:user/id @user-id}} query)
        patched-tag    (get result `tag-mut/patch-tag)
        fetched-tag    (tag-db/fetch-tag #:tag{:id tag-id :user-id @user-id})]
    (expect tag-id (:tag/id patched-tag))
    (expect new-tag-name (:tag/name patched-tag))
    (expect new-tag-colour (:tag/colour patched-tag))
    (expect inst? (:tag/created patched-tag))
    (expect #(.after % (:tag/updated tag)) (:tag/updated patched-tag))))

(defexpect ^:mutation ^:integration ^:tag normal-patch-tag-with-new-name
  (let [tag          (tag-db/create-tag {:tag/name "foo" :tag/user-id @user-id})
        tag-id       (:tag/id tag)
        new-tag-name "fim"
        query        [{`(tag-mut/patch-tag {:tag/id ~tag-id :tag/name ~new-tag-name}) tag-join}]
        result       (protected-parser {:request {:user/id @user-id}} query)
        patched-tag  (get result `tag-mut/patch-tag)]
    (expect tag-id (:tag/id patched-tag))
    (expect new-tag-name (:tag/name patched-tag))
    (expect nil (:tag/colour patched-tag))
    (expect inst? (:tag/created patched-tag))
    (expect #(.after % (:tag/updated tag)) (:tag/updated patched-tag))))

(defexpect ^:mutation ^:integration ^:tag normal-patch-tag-with-new-colour
  (let [tag            (tag-db/create-tag {:tag/name "foo" :tag/colour "#ffffff" :tag/user-id @user-id})
        tag-id         (:tag/id tag)
        new-tag-colour "#000000"
        query          [{`(tag-mut/patch-tag {:tag/id ~tag-id :tag/colour ~new-tag-colour}) tag-join}]
        result         (protected-parser {:request {:user/id @user-id}} query)
        patched-tag    (get result `tag-mut/patch-tag)
        fetched-tag    (tag-db/fetch-tag #:tag{:id tag-id :user-id @user-id})]
    (expect tag-id (:tag/id patched-tag))
    (expect (:tag/name tag) (:tag/name patched-tag))
    (expect new-tag-colour (:tag/colour patched-tag))
    (expect inst? (:tag/created patched-tag))
    (expect #(.after % (:tag/updated tag)) (:tag/updated patched-tag))))

(defexpect ^:mutation ^:integration ^:tag normal-patch-tag-without-new-name-and-colour
  (let [tag         (tag-db/create-tag {:tag/name "foo" :tag/colour "#ffffff" :tag/user-id @user-id})
        tag-id      (:tag/id tag)
        query       [{`(tag-mut/patch-tag {:tag/id ~tag-id}) tag-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        patched-tag (get result `tag-mut/patch-tag)
        fetched-tag (tag-db/fetch-tag #:tag{:id tag-id :user-id @user-id})]
    (expect tag-id (:tag/id patched-tag))
    (expect (:tag/name tag) (:tag/name patched-tag))
    (expect (:tag/colour tag) (:tag/colour fetched-tag))
    (expect inst? (:tag/created patched-tag))
    (expect #(.after % (:tag/updated tag)) (:tag/updated patched-tag))))

(defexpect ^:mutation ^:integration ^:tag fail-patch-tag-with-invalid-name
  (let [tag    (tag-db/create-tag {:tag/name "foo" :tag/user-id @user-id})
        tag-id (:tag/id tag)
        query  [{`(tag-mut/patch-tag {:tag/id ~tag-id :tag/name ""}) tag-join}]
        result (protected-parser {:request {:user/id @user-id}} query)
        error  (get result `tag-mut/patch-tag)]
    (expect {:error         true
             :error-type    :invalid-input
             :error-message "Invalid input"
             :error-data    {:tag/name ["should be at least 1 characters"]}}
            error)))

(defexpect ^:mutation ^:integration ^:tag fail-patch-tag-with-invalid-colour
  (let [tag    (tag-db/create-tag {:tag/name "foo" :tag/colour "#ffffff" :tag/user-id @user-id})
        tag-id (:tag/id tag)
        query  [{`(tag-mut/patch-tag {:tag/id ~tag-id :tag/colour ""}) tag-join}]
        result (protected-parser {:request {:user/id @user-id}} query)
        error  (get result `tag-mut/patch-tag)]
    (expect {:error         true
             :error-type    :invalid-input
             :error-message "Invalid input"
             :error-data    {:tag/colour ["should be at least 1 characters" "must have hex colour format"]}}
            error))
  (let [tag    (tag-db/create-tag {:tag/name "foo" :tag/colour "#ffffff" :tag/user-id @user-id})
        tag-id (:tag/id tag)
        query  [{`(tag-mut/patch-tag {:tag/id ~tag-id :tag/colour "foo"}) tag-join}]
        result (protected-parser {:request {:user/id @user-id}} query)
        error  (get result `tag-mut/patch-tag)]
    (expect {:error         true
             :error-type    :invalid-input
             :error-message "Invalid input"
             :error-data    {:tag/colour ["must have hex colour format"]}}
            error)))

(defexpect ^:mutation ^:integration ^:tag normal-delete-tag
  (let [tag         (tag-db/create-tag {:tag/name "foo" :tag/user-id @user-id})
        tag-id      (:tag/id tag)
        query       [{`(tag-mut/delete-tag {:tag/id ~tag-id}) tag-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        deleted-tag (get result `tag-mut/delete-tag)]
    (expect tag-id (:tag/id deleted-tag))
    (expect (:tag/name tag) (:tag/name deleted-tag))
    (expect nil (:tag/colour deleted-tag))
    (expect (:tag/created tag) (:tag/created deleted-tag))
    (expect (:tag/updated tag) (:tag/updated deleted-tag))))

(defexpect ^:mutation ^:integration ^:tag normal-delete-tag-with-nonexistent-id
  (let [tag-id      (random-uuid)
        query       [{`(tag-mut/delete-tag {:tag/id ~tag-id}) tag-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        deleted-tag (get result `tag-mut/delete-tag)]
    (expect nil deleted-tag)))

(defexpect ^:mutation ^:integration ^:tag fail-delete-tag-with-invalid-id
  (let [tag-id "foo"
        query  [{`(tag-mut/delete-tag {:tag/id ~tag-id}) tag-join}]
        result (protected-parser {:request {:user/id @user-id}} query)
        error  (get result `tag-mut/delete-tag)]
    (expect {:error         true
             :error-type    :invalid-input
             :error-message "Invalid input"
             :error-data    {:tag/id ["should be a uuid"]}}
            error)))

(defexpect ^:mutation ^:integration ^:tag normal-fetch-tags
  (let [tag1     (tag-db/create-tag {:tag/name "foo" :tag/colour "#fff" :tag/user-id @user-id})
        tag2     (tag-db/create-tag {:tag/name "foo1" :tag/colour "#000000" :tag/user-id @user-id})
        tag3     (tag-db/create-tag {:tag/name "bar" :tag/colour "#fafafa" :tag/user-id @user-id})
        expected {`tag-mut/fetch-tags {:user/tags (mapv trim-tag [tag1 tag2 tag3])}}
        query    [{`(tag-mut/fetch-tags {}) [:user/tags]}]
        actual   (protected-parser {:request {:user/id @user-id}} query)]
    (expect expected actual)))

(defexpect ^:mutation ^:integration ^:tag normal-fetch-tags-with-name-start
  (let [tag1     (tag-db/create-tag {:tag/name "foo" :tag/colour "#fff" :tag/user-id @user-id})
        tag2     (tag-db/create-tag {:tag/name "foo1" :tag/colour "#000000" :tag/user-id @user-id})
        tag3     (tag-db/create-tag {:tag/name "bar" :tag/colour "#fafafa" :tag/user-id @user-id})
        expected {`tag-mut/fetch-tags {:user/tags (mapv trim-tag [tag1 tag2])}}
        query    [{`(tag-mut/fetch-tags #:tag{:name "foo" :name-pos :start}) [:user/tags]}]
        actual   (protected-parser {:request {:user/id @user-id}} query)]
    (expect expected actual)))

(defexpect ^:mutation ^:integration ^:tag normal-fetch-tags-with-name-end
  (let [tag1     (tag-db/create-tag {:tag/name "foo" :tag/colour "#fff" :tag/user-id @user-id})
        tag2     (tag-db/create-tag {:tag/name "foo1" :tag/colour "#000000" :tag/user-id @user-id})
        tag3     (tag-db/create-tag {:tag/name "bao1" :tag/colour "#fafafa" :tag/user-id @user-id})
        expected {`tag-mut/fetch-tags {:user/tags (mapv trim-tag [tag2 tag3])}}
        query    [{`(tag-mut/fetch-tags #:tag{:name "o1" :name-pos :end}) [:user/tags]}]
        actual   (protected-parser {:request {:user/id @user-id}} query)]
    (expect expected actual)))

(defexpect ^:mutation ^:integration ^:tag normal-fetch-tags-with-name-between
  (let [tag1     (tag-db/create-tag {:tag/name "foo" :tag/colour "#fff" :tag/user-id @user-id})
        tag2     (tag-db/create-tag {:tag/name "gao1" :tag/colour "#000000" :tag/user-id @user-id})
        tag3     (tag-db/create-tag {:tag/name "boo1" :tag/colour "#fafafa" :tag/user-id @user-id})
        expected {`tag-mut/fetch-tags {:user/tags (mapv trim-tag [tag1 tag3])}}
        query1   [{`(tag-mut/fetch-tags #:tag{:name "oo" :name-pos :between}) [:user/tags]}]
        actual1  (protected-parser {:request {:user/id @user-id}} query1)
        query2   [{`(tag-mut/fetch-tags #:tag{:name "oo"}) [:user/tags]}]
        actual2  (protected-parser {:request {:user/id @user-id}} query1)]
    (expect expected actual1)
    (expect expected actual2)))

(defexpect ^:mutation ^:integration ^:tag normal-fetch-empty-tags
  (let [expected {`tag-mut/fetch-tags {:user/tags []}}
        query    [{`(tag-mut/fetch-tags #:tag{:name "oo" :name-pos :between}) [:user/tags]}]
        actual   (protected-parser {:request {:user/id @user-id}} query)]
    (expect expected actual)))

(comment
  (require '[kaocha.repl :as k])
  (require '[malli.core :as m])
  (m/validate [:map [:a :int] [:b {:optional true} [:maybe :int]]] {:a 3 :b 3})
  (k/run 'shinsetsu.mutations.tag-test)
  (k/run #'shinsetsu.mutations.tag-test/fail-delete-tag-with-invalid-id))
