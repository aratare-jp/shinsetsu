(ns shinsetsu.resolvers.tag-test
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.db.tag :as tag-db]
    [shinsetsu.db.tab :as tab-db]
    [shinsetsu.parser :refer [protected-parser]]
    [taoensso.timbre :as log]
    [com.wsscode.pathom.core :as pc]))

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
(defn trim-tag [tag] (select-keys tag tag-join))

(defn create-tag
  [tag]
  (-> tag
      (tag-db/create-tag)
      (dissoc :tag/user-id)))

(defexpect ^:db ^:integration ^:tag normal-fetch-tag
  (let [tag         (create-tag {:tag/name "foo" :tag/colour "#ffffff" :tag/user-id @user-id})
        tag-id      (:tag/id tag)
        fetched-tag (protected-parser {:request {:user/id @user-id}} [{[:tag/id tag-id] tag-join}])]
    (expect {[:tag/id tag-id] tag} fetched-tag)))

(defexpect ^:db ^:integration ^:tag normal-fetch-empty-tag
  (let [random-id (random-uuid)
        expected  {[:tag/id random-id] {:tag/id random-id}}
        actual    (protected-parser {:request {:user/id @user-id}} [{[:tag/id random-id] tag-join}])]
    (expect expected actual)))

(defexpect ^:db ^:integration ^:tag fail-fetch-invalid-tag
  (let [random-id   "foo"
        inner-error {:error         true
                     :error-type    :invalid-input
                     :error-message "Invalid input"
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

(defexpect ^:db ^:integration ^:tag fail-fetch-null-tag
  (let [random-id   nil
        inner-error {:error         true
                     :error-type    :invalid-input
                     :error-message "Invalid input"
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

(defexpect ^:resolver ^:integration ^:tag normal-fetch-tags
  (let [tag1     (tag-db/create-tag {:tag/name "foo" :tag/colour "#fff" :tag/user-id @user-id})
        tag2     (tag-db/create-tag {:tag/name "foo1" :tag/colour "#000000" :tag/user-id @user-id})
        tag3     (tag-db/create-tag {:tag/name "bar" :tag/colour "#fafafa" :tag/user-id @user-id})
        expected {:user/tags (mapv trim-tag [tag1 tag2 tag3])}
        query    [{:user/tags tag-join}]
        actual   (protected-parser {:request {:user/id @user-id}} query)]
    (expect expected actual)))

(defexpect ^:resolver ^:integration ^:tag normal-fetch-tags-with-name-start
  (let [tag1     (tag-db/create-tag {:tag/name "foo" :tag/colour "#fff" :tag/user-id @user-id})
        tag2     (tag-db/create-tag {:tag/name "foo1" :tag/colour "#000000" :tag/user-id @user-id})
        tag3     (tag-db/create-tag {:tag/name "bar" :tag/colour "#fafafa" :tag/user-id @user-id})
        expected {:user/tags (mapv trim-tag [tag1 tag2])}
        query    [`({:user/tags ~tag-join} {:tag/name "foo" :name-pos :start})]
        actual   (protected-parser {:request {:user/id @user-id}} query)]
    (expect expected actual)))

(defexpect ^:resolver ^:integration ^:tag normal-fetch-tags-with-name-end
  (let [tag1     (tag-db/create-tag {:tag/name "foo" :tag/colour "#fff" :tag/user-id @user-id})
        tag2     (tag-db/create-tag {:tag/name "foo1" :tag/colour "#000000" :tag/user-id @user-id})
        tag3     (tag-db/create-tag {:tag/name "bao1" :tag/colour "#fafafa" :tag/user-id @user-id})
        expected {:user/tags (mapv trim-tag [tag2 tag3])}
        query    [`({:user/tags ~tag-join} {:tag/name "o1" :name-pos :end})]
        actual   (protected-parser {:request {:user/id @user-id}} query)]
    (expect expected actual)))

(defexpect ^:resolver ^:integration ^:tag normal-fetch-tags-with-name-between
  (let [tag1     (tag-db/create-tag {:tag/name "foo" :tag/colour "#fff" :tag/user-id @user-id})
        tag2     (tag-db/create-tag {:tag/name "gao1" :tag/colour "#000000" :tag/user-id @user-id})
        tag3     (tag-db/create-tag {:tag/name "boo1" :tag/colour "#fafafa" :tag/user-id @user-id})
        expected {:user/tags (mapv trim-tag [tag1 tag3])}
        query1   [`({:user/tags ~tag-join} {:tag/name "oo" :name-pos :between})]
        actual1  (protected-parser {:request {:user/id @user-id}} query1)
        query2   [`({:user/tags ~tag-join} {:tag/name "oo"})]
        actual2  (protected-parser {:request {:user/id @user-id}} query2)]
    (expect expected actual1)
    (expect expected actual2)))

(defexpect ^:resolver ^:integration ^:tag normal-fetch-empty-tags
  (let [expected {:user/tags []}
        query    [{:user/tags tag-join}]
        actual   (protected-parser {:request {:user/id @user-id}} query)]
    (expect expected actual)))

(comment
  (require '[kaocha.repl :as k])
  (require '[shinsetsu.parser :refer [protected-parser]])
  (k/run-all)
  (k/run #'shinsetsu.resolvers.tag-test/normal-fetch-tags))
