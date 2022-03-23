(ns shinsetsu.mutations.bookmark-test
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [shinsetsu.parser :refer [protected-parser]]
    [shinsetsu.mutations.bookmark :as bookmark-mut]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.db.tab :as tab-db]
    [shinsetsu.db.tag :as tag-db]
    [shinsetsu.db.bookmark :as bookmark-db]
    [taoensso.timbre :as log]
    [buddy.hashers :as hashers])
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

(def bookmark-join [:bookmark/id :bookmark/title :bookmark/url :bookmark/image :bookmark/created :bookmark/updated])

(defexpect normal-create-bookmark
  (let [bookmark-title   "foo"
        bookmark-url     "bar"
        query            [{`(bookmark-mut/create-bookmark {:bookmark/title  ~bookmark-title
                                                           :bookmark/url    ~bookmark-url
                                                           :bookmark/tab-id ~(deref tab1-id)}) bookmark-join}]
        result           (protected-parser {:request {:user/id @user-id}} query)
        bookmark         (get result `bookmark-mut/create-bookmark)
        bookmark-id      (:bookmark/id bookmark)
        fetched-bookmark (bookmark-db/fetch-bookmark {:bookmark/id bookmark-id :user/id @user-id})]
    (expect (:bookmark/id fetched-bookmark) (:bookmark/id bookmark))
    (expect (:bookmark/title fetched-bookmark) (:bookmark/title bookmark))
    (expect (:bookmark/url fetched-bookmark) (:bookmark/url bookmark))
    (expect (:bookmark/image fetched-bookmark) (:bookmark/image bookmark))
    (expect (:bookmark/created fetched-bookmark) (:bookmark/created bookmark))
    (expect (:bookmark/updated fetched-bookmark) (:bookmark/updated bookmark))))

(defexpect fail-create-bookmark-with-invalid-title
  (let [bookmark-title ""
        bookmark-url   "bar"
        query          [{`(bookmark-mut/create-bookmark {:bookmark/title  ~bookmark-title
                                                         :bookmark/url    ~bookmark-url
                                                         :bookmark/tab-id ~(deref tab1-id)}) bookmark-join}]
        result         (protected-parser {:request {:user/id @user-id}} query)
        bookmark       (get result `bookmark-mut/create-bookmark)]
    (expect {:error         true
             :error-message "Invalid bookmark"
             :error-type    :invalid-input
             :error-data    {:bookmark/title ["should be at least 1 characters"]}} bookmark)))

(defexpect fail-create-bookmark-with-invalid-url
  (let [bookmark-title "foo"
        bookmark-url   ""
        query          [{`(bookmark-mut/create-bookmark {:bookmark/title  ~bookmark-title
                                                         :bookmark/url    ~bookmark-url
                                                         :bookmark/tab-id ~(deref tab1-id)}) bookmark-join}]
        result         (protected-parser {:request {:user/id @user-id}} query)
        bookmark       (get result `bookmark-mut/create-bookmark)]
    (expect {:error         true
             :error-message "Invalid bookmark"
             :error-type    :invalid-input
             :error-data    {:bookmark/url ["should be at least 1 characters"]}} bookmark)))

(defexpect fail-create-bookmark-with-invalid-tab
  (let [bookmark-title "foo"
        bookmark-url   "bar"
        tab-id         "foo"
        query          [{`(bookmark-mut/create-bookmark {:bookmark/title  ~bookmark-title
                                                         :bookmark/url    ~bookmark-url
                                                         :bookmark/tab-id ~tab-id}) bookmark-join}]
        result         (protected-parser {:request {:user/id @user-id}} query)
        bookmark       (get result `bookmark-mut/create-bookmark)]
    (expect {:error         true
             :error-message "Invalid bookmark"
             :error-type    :invalid-input
             :error-data    {:bookmark/tab-id ["should be a uuid"]}} bookmark)))

(defexpect fail-create-bookmark-with-nonexistent-tab
  (let [bookmark-title "foo"
        bookmark-url   "bar"
        tab-id         (UUID/randomUUID)
        query          [{`(bookmark-mut/create-bookmark {:bookmark/title  ~bookmark-title
                                                         :bookmark/url    ~bookmark-url
                                                         :bookmark/tab-id ~tab-id}) bookmark-join}]
        result         (protected-parser {:request {:user/id @user-id}} query)
        bookmark       (get result `bookmark-mut/create-bookmark)]
    (expect {:error         true
             :error-message "Nonexistent tab"
             :error-type    :invalid-input
             :error-data    {:bookmark/tab-id ["nonexistent"]}} bookmark)))

(defexpect normal-patch-bookmark-with-new-title-and-url
  (let [bookmark           (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                         :bookmark/url     "bar"
                                                         :bookmark/tab-id  @tab1-id
                                                         :bookmark/user-id @user-id})
        new-bookmark-title "fim"
        new-bookmark-url   "boo"
        bookmark-id        (:bookmark/id bookmark)
        query              [{`(bookmark-mut/patch-bookmark {:bookmark/id     ~bookmark-id
                                                            :bookmark/title  ~new-bookmark-title
                                                            :bookmark/url    ~new-bookmark-url
                                                            :bookmark/tab-id ~(deref tab1-id)}) bookmark-join}]
        result             (protected-parser {:request {:user/id @user-id}} query)
        patched-bookmark   (get result `bookmark-mut/patch-bookmark)
        fetched-bookmark   (bookmark-db/fetch-bookmark {:bookmark/id bookmark-id :user/id @user-id})]
    (expect (:bookmark/id fetched-bookmark) (:bookmark/id patched-bookmark))
    (expect new-bookmark-title (:bookmark/title patched-bookmark))
    (expect new-bookmark-url (:bookmark/url patched-bookmark))
    (expect (:bookmark/image fetched-bookmark) (:bookmark/image patched-bookmark))
    (expect (:bookmark/created fetched-bookmark) (:bookmark/created patched-bookmark))
    (expect (:bookmark/updated fetched-bookmark) (:bookmark/updated patched-bookmark))
    (expect #(.after % (:bookmark/updated bookmark)) (:bookmark/updated patched-bookmark))))

(defexpect normal-patch-bookmark-with-new-title
  (let [bookmark           (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                         :bookmark/url     "bar"
                                                         :bookmark/tab-id  @tab1-id
                                                         :bookmark/user-id @user-id})
        new-bookmark-title "fim"
        bookmark-id        (:bookmark/id bookmark)
        query              [{`(bookmark-mut/patch-bookmark {:bookmark/id     ~bookmark-id
                                                            :bookmark/title  ~new-bookmark-title
                                                            :bookmark/tab-id ~(deref tab1-id)}) bookmark-join}]
        result             (protected-parser {:request {:user/id @user-id}} query)
        patched-bookmark   (get result `bookmark-mut/patch-bookmark)
        fetched-bookmark   (bookmark-db/fetch-bookmark {:bookmark/id bookmark-id :user/id @user-id})]
    (expect (:bookmark/id fetched-bookmark) (:bookmark/id patched-bookmark))
    (expect new-bookmark-title (:bookmark/title patched-bookmark))
    (expect (:bookmark/url fetched-bookmark) (:bookmark/url patched-bookmark))
    (expect (:bookmark/image fetched-bookmark) (:bookmark/image patched-bookmark))
    (expect (:bookmark/created fetched-bookmark) (:bookmark/created patched-bookmark))
    (expect (:bookmark/updated fetched-bookmark) (:bookmark/updated patched-bookmark))
    (expect #(.after % (:bookmark/updated bookmark)) (:bookmark/updated patched-bookmark))))

(defexpect normal-patch-bookmark-with-new-url
  (let [bookmark         (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                       :bookmark/url     "bar"
                                                       :bookmark/tab-id  @tab1-id
                                                       :bookmark/user-id @user-id})
        new-bookmark-url "boo"
        bookmark-id      (:bookmark/id bookmark)
        query            [{`(bookmark-mut/patch-bookmark {:bookmark/id     ~bookmark-id
                                                          :bookmark/url    ~new-bookmark-url
                                                          :bookmark/tab-id ~(deref tab1-id)}) bookmark-join}]
        result           (protected-parser {:request {:user/id @user-id}} query)
        patched-bookmark (get result `bookmark-mut/patch-bookmark)
        fetched-bookmark (bookmark-db/fetch-bookmark {:bookmark/id bookmark-id :user/id @user-id})]
    (expect (:bookmark/id fetched-bookmark) (:bookmark/id patched-bookmark))
    (expect (:bookmark/title fetched-bookmark) (:bookmark/title patched-bookmark))
    (expect new-bookmark-url (:bookmark/url patched-bookmark))
    (expect (:bookmark/image fetched-bookmark) (:bookmark/image patched-bookmark))
    (expect (:bookmark/created fetched-bookmark) (:bookmark/created patched-bookmark))
    (expect (:bookmark/updated fetched-bookmark) (:bookmark/updated patched-bookmark))
    (expect #(.after % (:bookmark/updated bookmark)) (:bookmark/updated patched-bookmark))))

(defexpect normal-patch-bookmark-with-new-tab
  (let [bookmark         (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                       :bookmark/url     "bar"
                                                       :bookmark/tab-id  @tab1-id
                                                       :bookmark/user-id @user-id})
        bookmark-id      (:bookmark/id bookmark)
        query            [{`(bookmark-mut/patch-bookmark {:bookmark/id     ~bookmark-id
                                                          :bookmark/tab-id ~(deref tab2-id)}) bookmark-join}]
        result           (protected-parser {:request {:user/id @user-id}} query)
        patched-bookmark (get result `bookmark-mut/patch-bookmark)
        fetched-bookmark (bookmark-db/fetch-bookmark {:bookmark/id bookmark-id :user/id @user-id})]
    (expect (:bookmark/id fetched-bookmark) (:bookmark/id patched-bookmark))
    (expect (:bookmark/title fetched-bookmark) (:bookmark/title patched-bookmark))
    (expect (:bookmark/url fetched-bookmark) (:bookmark/url patched-bookmark))
    (expect (:bookmark/image fetched-bookmark) (:bookmark/image patched-bookmark))
    (expect @tab2-id (:bookmark/tab-id fetched-bookmark))
    (expect (:bookmark/created fetched-bookmark) (:bookmark/created patched-bookmark))
    (expect (:bookmark/updated fetched-bookmark) (:bookmark/updated patched-bookmark))
    (expect #(.after % (:bookmark/updated bookmark)) (:bookmark/updated patched-bookmark))))

(defexpect fail-patch-bookmark-with-invalid-title
  (let [bookmark       (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                     :bookmark/url     "bar"
                                                     :bookmark/tab-id  @tab1-id
                                                     :bookmark/user-id @user-id})
        bookmark-id    (:bookmark/id bookmark)
        bookmark-title ""
        query          [{`(bookmark-mut/patch-bookmark {:bookmark/id     ~bookmark-id
                                                        :bookmark/title  ~bookmark-title
                                                        :bookmark/tab-id ~(deref tab1-id)}) bookmark-join}]
        result         (protected-parser {:request {:user/id @user-id}} query)
        bookmark       (get result `bookmark-mut/patch-bookmark)]
    (expect {:error         true
             :error-message "Invalid bookmark"
             :error-type    :invalid-input
             :error-data    {:bookmark/title ["should be at least 1 characters"]}})))

(defexpect fail-patch-bookmark-with-invalid-url
  (let [bookmark     (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                   :bookmark/url     "bar"
                                                   :bookmark/tab-id  @tab1-id
                                                   :bookmark/user-id @user-id})
        bookmark-id  (:bookmark/id bookmark)
        bookmark-url ""
        query        [{`(bookmark-mut/patch-bookmark {:bookmark/id     ~bookmark-id
                                                      :bookmark/url    ~bookmark-url
                                                      :bookmark/tab-id ~(deref tab1-id)}) bookmark-join}]
        result       (protected-parser {:request {:user/id @user-id}} query)
        bookmark     (get result `bookmark-mut/patch-bookmark)]
    (expect {:error         true
             :error-message "Invalid bookmark"
             :error-type    :invalid-input
             :error-data    {:bookmark/url ["should be at least 1 characters"]}})))

(defexpect fail-patch-bookmark-with-invalid-tab
  (let [bookmark    (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                  :bookmark/url     "bar"
                                                  :bookmark/tab-id  @tab1-id
                                                  :bookmark/user-id @user-id})
        bookmark-id (:bookmark/id bookmark)
        tab-id      "foo"
        query       [{`(bookmark-mut/patch-bookmark {:bookmark/id     ~bookmark-id
                                                     :bookmark/title  "foo"
                                                     :bookmark/tab-id ~tab-id}) bookmark-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        bookmark    (get result `bookmark-mut/patch-bookmark)]
    (expect {:error         true
             :error-message "Invalid bookmark"
             :error-type    :invalid-input
             :error-data    {:bookmark/tab-id ["should be a uuid"]}} bookmark)))

(defexpect fail-patch-bookmark-with-nonexistent-tab
  (let [bookmark    (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                  :bookmark/url     "bar"
                                                  :bookmark/tab-id  @tab1-id
                                                  :bookmark/user-id @user-id})
        bookmark-id (:bookmark/id bookmark)
        tab-id      (UUID/randomUUID)
        query       [{`(bookmark-mut/patch-bookmark {:bookmark/id     ~bookmark-id
                                                     :bookmark/tab-id ~tab-id}) bookmark-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        bookmark    (get result `bookmark-mut/patch-bookmark)]
    (expect {:error         true
             :error-message "Nonexistent tab"
             :error-type    :invalid-input
             :error-data    {:bookmark/tab-id ["nonexistent"]}} bookmark)))

(defexpect normal-delete-bookmark
  (let [bookmark         (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                       :bookmark/url     "bar"
                                                       :bookmark/user-id @user-id
                                                       :bookmark/tab-id  @tab1-id})
        bookmark-id      (:bookmark/id bookmark)
        query            [{`(bookmark-mut/delete-bookmark {:bookmark/id ~bookmark-id}) bookmark-join}]
        result           (protected-parser {:request {:user/id @user-id}} query)
        deleted-bookmark (get result `bookmark-mut/delete-bookmark)
        fetched-bookmark (bookmark-db/fetch-bookmark {:bookmark/id bookmark-id :user/id @user-id})]
    (expect nil fetched-bookmark)
    (expect (:bookmark/id deleted-bookmark) (:bookmark/id bookmark))
    (expect (:bookmark/title deleted-bookmark) (:bookmark/title bookmark))
    (expect (:bookmark/url deleted-bookmark) (:bookmark/url bookmark))
    (expect (:bookmark/image deleted-bookmark) (:bookmark/image bookmark))
    (expect (:bookmark/created deleted-bookmark) (:bookmark/created bookmark))
    (expect (:bookmark/updated deleted-bookmark) (:bookmark/updated bookmark))))

(defexpect normal-delete-nonexistent-tab
  (let [bookmark-id (UUID/randomUUID)
        tab-id      "foo"
        query       [{`(bookmark-mut/delete-bookmark {:bookmark/id ~bookmark-id}) bookmark-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        bookmark    (get result `bookmark-mut/patch-bookmark)]
    (expect nil bookmark)))

(defexpect fail-delete-tab-with-invalid-id
  (let [bookmark-id "foo"
        query       [{`(bookmark-mut/delete-bookmark {:bookmark/id ~bookmark-id}) bookmark-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        bookmark    (get result `bookmark-mut/delete-bookmark)]
    (expect {:error         true
             :error-message "Invalid bookmark"
             :error-type    :invalid-input
             :error-data    {:bookmark/id ["should be a uuid"]}} bookmark)))

(defexpect normal-create-bookmark-tag
  (let [bookmark    (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                  :bookmark/url     "bar"
                                                  :bookmark/user-id @user-id
                                                  :bookmark/tab-id  @tab1-id})
        bookmark-id (:bookmark/id bookmark)
        tag         (tag-db/create-tag {:tag/name "foo" :tag/user-id @user-id})
        tag-id      (:tag/id tag)
        query       [{`(bookmark-mut/create-bookmark-tag {:bookmark/id ~bookmark-id :tag/id ~tag-id}) bookmark-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        _           (get result `bookmark-mut/create-bookmark-tag)
        fetched     (bookmark-db/fetch-tags-by-bookmark {:bookmark/id bookmark-id :user/id @user-id})]
    (expect 1 (count fetched))
    (expect tag-id (-> fetched first :bookmark-tag/tag-id))
    (expect bookmark-id (-> fetched first :bookmark-tag/bookmark-id))
    (expect @user-id (-> fetched first :bookmark-tag/user-id))))

(defexpect fail-create-bookmark-tag-with-invalid-bookmark
  (let [bookmark-id "foo"
        tag         (tag-db/create-tag {:tag/name "foo" :tag/user-id @user-id})
        tag-id      (:tag/id tag)
        query       [{`(bookmark-mut/create-bookmark-tag {:bookmark/id ~bookmark-id :tag/id ~tag-id}) bookmark-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        error       (get result `bookmark-mut/create-bookmark-tag)]
    (expect {:error         true
             :error-message "Invalid bookmark or tag"
             :error-type    :invalid-input
             :error-data    {:bookmark/id ["should be a uuid"]}} error)))

(defexpect fail-create-bookmark-tag-with-nonexistent-bookmark
  (let [bookmark-id (UUID/randomUUID)
        tag         (tag-db/create-tag {:tag/name "foo" :tag/user-id @user-id})
        tag-id      (:tag/id tag)
        query       [{`(bookmark-mut/create-bookmark-tag {:bookmark/id ~bookmark-id :tag/id ~tag-id}) bookmark-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        error       (get result `bookmark-mut/create-bookmark-tag)]
    (expect {:error         true
             :error-message "Nonexistent bookmark or tag"
             :error-type    :invalid-input
             :error-data    {:bookmark/id ["nonexistent"] :tag/id ["nonexistent"]}} error)))

(defexpect fail-create-bookmark-tag-with-invalid-tag
  (let [bookmark    (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                  :bookmark/url     "bar"
                                                  :bookmark/user-id @user-id
                                                  :bookmark/tab-id  @tab1-id})
        bookmark-id (:bookmark/id bookmark)
        tag-id      "foo"
        query       [{`(bookmark-mut/create-bookmark-tag {:bookmark/id ~bookmark-id :tag/id ~tag-id}) bookmark-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        error       (get result `bookmark-mut/create-bookmark-tag)]
    (expect {:error         true
             :error-message "Invalid bookmark or tag"
             :error-type    :invalid-input
             :error-data    {:tag/id ["should be a uuid"]}} error)))

(defexpect fail-create-bookmark-tag-with-nonexistent-tag
  (let [bookmark    (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                  :bookmark/url     "bar"
                                                  :bookmark/user-id @user-id
                                                  :bookmark/tab-id  @tab1-id})
        bookmark-id (:bookmark/id bookmark)
        tag-id      (UUID/randomUUID)
        query       [{`(bookmark-mut/create-bookmark-tag {:bookmark/id ~bookmark-id :tag/id ~tag-id}) bookmark-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        error       (get result `bookmark-mut/create-bookmark-tag)]
    (expect {:error         true
             :error-message "Nonexistent bookmark or tag"
             :error-type    :invalid-input
             :error-data    {:bookmark/id ["nonexistent"] :tag/id ["nonexistent"]}} error)))


(defexpect normal-delete-bookmark-tag
  (let [bookmark              (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                            :bookmark/url     "bar"
                                                            :bookmark/user-id @user-id
                                                            :bookmark/tab-id  @tab1-id})
        bookmark-id           (:bookmark/id bookmark)
        tag                   (tag-db/create-tag {:tag/name "foo" :tag/user-id @user-id})
        tag-id                (:tag/id tag)
        bookmark-tag          (bookmark-db/create-bookmark-tag {:bookmark/id bookmark-id
                                                                :tag/id      tag-id
                                                                :user/id     @user-id})
        query                 [{`(bookmark-mut/delete-bookmark-tag {:bookmark/id ~bookmark-id :tag/id ~tag-id})
                                [:bookmark/id :tag/id]}]
        result                (protected-parser {:request {:user/id @user-id}} query)
        bookmark-tag          (get result `bookmark-mut/delete-bookmark-tag)
        fetched-bookmark-tags (bookmark-db/fetch-tags-by-bookmark {:bookmark/id bookmark-id :user/id @user-id})]
    (expect [] fetched-bookmark-tags)
    (expect tag-id (:tag/id bookmark-tag))
    (expect bookmark-id (:bookmark/id bookmark-tag))))

(defexpect normal-delete-bookmark-tag-with-nonexistent-bookmark
  (let [bookmark-id (UUID/randomUUID)
        tag         (tag-db/create-tag {:tag/name "foo" :tag/user-id @user-id})
        tag-id      (:tag/id tag)
        query       [{`(bookmark-mut/delete-bookmark-tag {:bookmark/id ~bookmark-id :tag/id ~tag-id})
                      [:bookmark/id :tag/id]}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        actual      (get result `bookmark-mut/delete-bookmark-tag)]
    (expect {:tag/id tag-id :bookmark/id bookmark-id} actual)))

(defexpect normal-delete-bookmark-tag-with-nonexistent-tag
  (let [bookmark    (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                  :bookmark/url     "bar"
                                                  :bookmark/user-id @user-id
                                                  :bookmark/tab-id  @tab1-id})
        bookmark-id (:bookmark/id bookmark)
        tag-id      (UUID/randomUUID)
        query       [{`(bookmark-mut/delete-bookmark-tag {:bookmark/id ~bookmark-id :tag/id ~tag-id})
                      [:bookmark/id :tag/id]}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        actual      (get result `bookmark-mut/delete-bookmark-tag)]
    (expect {:tag/id tag-id :bookmark/id bookmark-id} actual)))

(defexpect fail-delete-bookmark-tag-with-invalid-bookmark
  (let [bookmark-id "foo"
        tag         (tag-db/create-tag {:tag/name "foo" :tag/user-id @user-id})
        tag-id      (:tag/id tag)
        query       [{`(bookmark-mut/delete-bookmark-tag {:bookmark/id ~bookmark-id :tag/id ~tag-id})
                      [:bookmark/id :tag/id]}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        error       (get result `bookmark-mut/delete-bookmark-tag)]
    (expect {:error         true
             :error-message "Invalid bookmark or tag"
             :error-type    :invalid-input
             :error-data    {:bookmark/id ["should be a uuid"]}} error)))

(defexpect fail-delete-bookmark-tag-with-invalid-tag
  (let [bookmark    (bookmark-db/create-bookmark {:bookmark/title   "foo"
                                                  :bookmark/url     "bar"
                                                  :bookmark/user-id @user-id
                                                  :bookmark/tab-id  @tab1-id})
        bookmark-id (:bookmark/id bookmark)
        tag-id      "foo"
        query       [{`(bookmark-mut/delete-bookmark-tag {:bookmark/id ~bookmark-id :tag/id ~tag-id})
                      [:bookmark/id :tag/id]}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        error       (get result `bookmark-mut/delete-bookmark-tag)]
    (expect {:error         true
             :error-message "Invalid bookmark or tag"
             :error-type    :invalid-input
             :error-data    {:tag/id ["should be a uuid"]}} error)))

(comment
  (require '[kaocha.repl :as k])
  (k/run 'shinsetsu.mutations.bookmark-test)
  (k/run #'shinsetsu.mutations.bookmark-test/fail-create-bookmark-tag-with-nonexistent-tag))
