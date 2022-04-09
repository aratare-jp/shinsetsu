(ns shinsetsu.mutations.bookmark-test
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [shinsetsu.parser :refer [protected-parser]]
    [shinsetsu.mutations.bookmark :as bmut]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.db.tab :as tab-db]
    [shinsetsu.db.tag :as tag-db]
    [shinsetsu.db.bookmark :as bdb]
    [shinsetsu.db.bookmark-tag :as btdb]
    [taoensso.timbre :as log]
    [buddy.hashers :as hashers]
    [com.fulcrologic.fulcro.algorithms.tempid :as tempid]))

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

(def bookmark-join [:bookmark/id :bookmark/title :bookmark/url :bookmark/image :bookmark/favourite :bookmark/created
                    :bookmark/updated :bookmark/tags])
(defn trim-bookmark [b] (dissoc b :bookmark/user-id :bookmark/tab-id))

(defexpect ^:mutation ^:bookmark ^:integration normal-create-bookmark
  (let [bookmark-title "foo"
        bookmark-url   "bar"
        query          [{`(bmut/create-bookmark #:bookmark{:title ~bookmark-title :url ~bookmark-url :tab-id ~(deref tab1-id)})
                         bookmark-join}]
        actual         (protected-parser {:request {:user/id @user-id}} query)
        bookmark       (get actual `bmut/create-bookmark)
        bookmark-id    (:bookmark/id bookmark)
        expected       {`bmut/create-bookmark (-> #:bookmark{:id bookmark-id :user-id @user-id}
                                                  bdb/fetch-bookmark
                                                  trim-bookmark
                                                  (assoc :bookmark/tags []))}]
    (expect expected actual)))

(defexpect ^:mutation ^:bookmark ^:integration normal-create-bookmark-with-tempid
  (let [tempid         (tempid/tempid)
        bookmark-title "foo"
        bookmark-url   "bar"
        query          [{`(bmut/create-bookmark #:bookmark{:id ~tempid :title ~bookmark-title :url ~bookmark-url :tab-id ~(deref tab1-id)})
                         bookmark-join}]
        actual         (protected-parser {:request {:user/id @user-id}} query)
        bookmark       (get actual `bmut/create-bookmark)
        bookmark-id    (:bookmark/id bookmark)
        expected       {`bmut/create-bookmark (-> #:bookmark{:id bookmark-id :user-id @user-id}
                                                  bdb/fetch-bookmark
                                                  trim-bookmark
                                                  (merge {:tempids {tempid bookmark-id} :bookmark/tags []}))}]
    (expect expected actual)))

(defexpect ^:mutation ^:bookmark ^:integration fail-create-bookmark-with-invalid-title
  (let [title    ""
        url      "bar"
        query    [{`(bmut/create-bookmark #:bookmark{:title ~title :url ~url :tab-id ~(deref tab1-id)}) bookmark-join}]
        result   (protected-parser {:request {:user/id @user-id}} query)
        bookmark (get result `bmut/create-bookmark)]
    (expect {:error         true
             :error-message "Invalid input"
             :error-type    :invalid-input
             :error-data    {:bookmark/title ["should be at least 1 characters"]}} bookmark)))

(defexpect ^:mutation ^:bookmark ^:integration fail-create-bookmark-with-invalid-url
  (let [title    "foo"
        url      ""
        query    [{`(bmut/create-bookmark #:bookmark{:title ~title :url ~url :tab-id ~(deref tab1-id)}) bookmark-join}]
        result   (protected-parser {:request {:user/id @user-id}} query)
        bookmark (get result `bmut/create-bookmark)]
    (expect {:error         true
             :error-message "Invalid input"
             :error-type    :invalid-input
             :error-data    {:bookmark/url ["should be at least 1 characters"]}} bookmark)))

(defexpect ^:mutation ^:bookmark ^:integration fail-create-bookmark-with-invalid-tab
  (let [title    "foo"
        url      "bar"
        tab-id   "foo"
        query    [{`(bmut/create-bookmark #:bookmark{:title ~title :url ~url :tab-id ~tab-id}) bookmark-join}]
        result   (protected-parser {:request {:user/id @user-id}} query)
        bookmark (get result `bmut/create-bookmark)]
    (expect {:error         true
             :error-message "Invalid input"
             :error-type    :invalid-input
             :error-data    {:bookmark/tab-id ["should be a uuid"]}} bookmark)))

(defexpect ^:mutation ^:bookmark ^:integration fail-create-bookmark-with-nonexistent-tab
  (let [title    "foo"
        url      "bar"
        tab-id   (random-uuid)
        query    [{`(bmut/create-bookmark #:bookmark{:title ~title :url ~url :tab-id ~tab-id}) bookmark-join}]
        result   (protected-parser {:request {:user/id @user-id}} query)
        bookmark (get result `bmut/create-bookmark)]
    (expect {:error         true
             :error-message "Nonexistent tab"
             :error-type    :invalid-input
             :error-data    {:bookmark/tab-id ["nonexistent"]}} bookmark)))

(defexpect ^:mutation ^:bookmark ^:integration normal-patch-bookmark-with-new-title-and-url
  (let [bookmark    (bdb/create-bookmark #:bookmark{:title "foo" :url "bar" :tab-id @tab1-id :user-id @user-id})
        new-title   "fim"
        new-url     "boo"
        bookmark-id (:bookmark/id bookmark)
        query       [{`(bmut/patch-bookmark #:bookmark{:id ~bookmark-id :title ~new-title :url ~new-url :tab-id ~(deref tab1-id)}) bookmark-join}]
        actual      (-> (protected-parser {:request {:user/id @user-id}} query) (get `bmut/patch-bookmark))
        expected    (bdb/fetch-bookmark {:bookmark/id bookmark-id :bookmark/user-id @user-id})]
    (expect (:bookmark/id expected) (:bookmark/id actual))
    (expect new-title (:bookmark/title actual))
    (expect new-url (:bookmark/url actual))
    (expect (:bookmark/image expected) (:bookmark/image actual))
    (expect (:bookmark/created expected) (:bookmark/created actual))
    (expect (:bookmark/updated expected) (:bookmark/updated actual))
    (expect #(.after % (:bookmark/updated bookmark)) (:bookmark/updated actual))))

(defexpect ^:mutation ^:bookmark ^:integration normal-patch-bookmark-with-new-title
  (let [bookmark    (bdb/create-bookmark #:bookmark{:title "foo" :url "bar" :tab-id @tab1-id :user-id @user-id})
        new-title   "fim"
        bookmark-id (:bookmark/id bookmark)
        query       [{`(bmut/patch-bookmark #:bookmark{:id ~bookmark-id :title ~new-title :tab-id ~(deref tab1-id)}) bookmark-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        actual      (get result `bmut/patch-bookmark)
        expected    (bdb/fetch-bookmark {:bookmark/id bookmark-id :bookmark/user-id @user-id})]
    (expect (:bookmark/id expected) (:bookmark/id actual))
    (expect new-title (:bookmark/title actual))
    (expect (:bookmark/url expected) (:bookmark/url actual))
    (expect (:bookmark/image expected) (:bookmark/image actual))
    (expect (:bookmark/created expected) (:bookmark/created actual))
    (expect (:bookmark/updated expected) (:bookmark/updated actual))
    (expect #(.after % (:bookmark/updated bookmark)) (:bookmark/updated actual))))

(defexpect ^:mutation ^:bookmark ^:integration normal-patch-bookmark-with-new-url
  (let [bookmark    (bdb/create-bookmark #:bookmark{:title "foo" :url "bar" :tab-id @tab1-id :user-id @user-id})
        new-url     "boo"
        bookmark-id (:bookmark/id bookmark)
        query       [{`(bmut/patch-bookmark #:bookmark{:id ~bookmark-id :url ~new-url :tab-id ~(deref tab1-id)}) bookmark-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        actual      (get result `bmut/patch-bookmark)
        expected    (bdb/fetch-bookmark {:bookmark/id bookmark-id :bookmark/user-id @user-id})]
    (expect (:bookmark/id expected) (:bookmark/id actual))
    (expect (:bookmark/title expected) (:bookmark/title actual))
    (expect new-url (:bookmark/url actual))
    (expect (:bookmark/image expected) (:bookmark/image actual))
    (expect (:bookmark/created expected) (:bookmark/created actual))
    (expect (:bookmark/updated expected) (:bookmark/updated actual))
    (expect #(.after % (:bookmark/updated bookmark)) (:bookmark/updated actual))))

(defexpect ^:mutation ^:bookmark ^:integration normal-patch-bookmark-with-new-tab
  (let [bookmark    (bdb/create-bookmark #:bookmark{:title "foo" :url "bar" :tab-id @tab1-id :user-id @user-id})
        bookmark-id (:bookmark/id bookmark)
        query       [{`(bmut/patch-bookmark #:bookmark{:id ~bookmark-id :tab-id ~(deref tab2-id)}) bookmark-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        actual      (get result `bmut/patch-bookmark)
        expected    (bdb/fetch-bookmark {:bookmark/id bookmark-id :bookmark/user-id @user-id})]
    (expect (:bookmark/id expected) (:bookmark/id actual))
    (expect (:bookmark/title expected) (:bookmark/title actual))
    (expect (:bookmark/url expected) (:bookmark/url actual))
    (expect (:bookmark/image expected) (:bookmark/image actual))
    (expect @tab2-id (:bookmark/tab-id expected))
    (expect (:bookmark/created expected) (:bookmark/created actual))
    (expect (:bookmark/updated expected) (:bookmark/updated actual))
    (expect #(.after % (:bookmark/updated bookmark)) (:bookmark/updated actual))))

(defexpect ^:mutation ^:bookmark ^:integration normal-patch-bookmark-with-new-tags
  (let [bookmark    (bdb/create-bookmark #:bookmark{:title "foo" :url "bar" :tab-id @tab1-id :user-id @user-id})
        bookmark-id (:bookmark/id bookmark)
        tag1-id     (:tag/id (tag-db/create-tag #:tag{:name "foo" :user-id @user-id}))
        tag2-id     (:tag/id (tag-db/create-tag #:tag{:name "bar" :user-id @user-id}))
        query       [{`(bmut/patch-bookmark #:bookmark{:id ~bookmark-id :add-tags [~tag1-id ~tag2-id]}) bookmark-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        actual      (get result `bmut/patch-bookmark)
        expected    (bdb/fetch-bookmark #:bookmark{:id bookmark-id :user-id @user-id})]
    (expect (:bookmark/id expected) (:bookmark/id actual))
    (expect (:bookmark/title expected) (:bookmark/title actual))
    (expect (:bookmark/url expected) (:bookmark/url actual))
    (expect (:bookmark/image expected) (:bookmark/image actual))
    (expect @tab1-id (:bookmark/tab-id expected))
    (expect [{:tag/id tag1-id} {:tag/id tag2-id}] (:bookmark/tags actual))
    (expect (:bookmark/created expected) (:bookmark/created actual))
    (expect (:bookmark/updated expected) (:bookmark/updated actual))
    (expect #(.after % (:bookmark/updated bookmark)) (:bookmark/updated actual))))

(defexpect ^:mutation ^:bookmark ^:integration normal-patch-bookmark-with-less-tags
  (let [bookmark    (bdb/create-bookmark #:bookmark{:title "foo" :url "bar" :tab-id @tab1-id :user-id @user-id})
        bookmark-id (:bookmark/id bookmark)
        tag1-id     (:tag/id (tag-db/create-tag #:tag{:name "foo" :user-id @user-id}))
        tag2-id     (:tag/id (tag-db/create-tag #:tag{:name "bar" :user-id @user-id}))
        _           (btdb/create-bookmark-tags #:bookmark-tag{:bookmark-id bookmark-id :tag-ids [tag1-id tag2-id] :user-id @user-id})
        query       [{`(bmut/patch-bookmark #:bookmark{:id ~bookmark-id :remove-tags [~tag2-id]}) bookmark-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        actual      (get result `bmut/patch-bookmark)
        expected    (bdb/fetch-bookmark #:bookmark{:id bookmark-id :user-id @user-id})]
    (expect (:bookmark/id expected) (:bookmark/id actual))
    (expect (:bookmark/title expected) (:bookmark/title actual))
    (expect (:bookmark/url expected) (:bookmark/url actual))
    (expect (:bookmark/image expected) (:bookmark/image actual))
    (expect @tab1-id (:bookmark/tab-id expected))
    (expect [{:tag/id tag1-id}] (:bookmark/tags actual))
    (expect (:bookmark/created expected) (:bookmark/created actual))
    (expect (:bookmark/updated expected) (:bookmark/updated actual))
    (expect #(.after % (:bookmark/updated bookmark)) (:bookmark/updated actual))))

(defexpect ^:mutation ^:bookmark ^:integration fail-patch-bookmark-with-invalid-title
  (let [bookmark    (bdb/create-bookmark #:bookmark{:title "foo" :url "bar" :tab-id @tab1-id :user-id @user-id})
        bookmark-id (:bookmark/id bookmark)
        title       ""
        query       [{`(bmut/patch-bookmark #:bookmark{:id ~bookmark-id :title ~title :tab-id ~(deref tab1-id)}) bookmark-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        bookmark    (get result `bmut/patch-bookmark)]
    (expect {:error         true
             :error-message "Invalid input"
             :error-type    :invalid-input
             :error-data    {:bookmark/title ["should be at least 1 characters"]}})))

(defexpect ^:mutation ^:bookmark ^:integration fail-patch-bookmark-with-invalid-url
  (let [bookmark    (bdb/create-bookmark #:bookmark{:title "foo" :url "bar" :tab-id @tab1-id :user-id @user-id})
        bookmark-id (:bookmark/id bookmark)
        url         ""
        query       [{`(bmut/patch-bookmark #:bookmark{:id ~bookmark-id :url ~url :tab-id ~(deref tab1-id)}) bookmark-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        bookmark    (get result `bmut/patch-bookmark)]
    (expect {:error         true
             :error-message "Invalid input"
             :error-type    :invalid-input
             :error-data    {:bookmark/url ["should be at least 1 characters"]}})))

(defexpect ^:mutation ^:bookmark ^:integration fail-patch-bookmark-with-invalid-tab
  (let [bookmark    (bdb/create-bookmark #:bookmark{:title "foo" :url "bar" :tab-id @tab1-id :user-id @user-id})
        bookmark-id (:bookmark/id bookmark)
        tab-id      "foo"
        query       [{`(bmut/patch-bookmark #:bookmark{:id ~bookmark-id :title "foo" :tab-id ~tab-id}) bookmark-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        bookmark    (get result `bmut/patch-bookmark)]
    (expect {:error         true
             :error-message "Invalid input"
             :error-type    :invalid-input
             :error-data    {:bookmark/tab-id ["should be a uuid"]}} bookmark)))

(defexpect ^:mutation ^:bookmark ^:integration fail-patch-bookmark-with-nonexistent-tab
  (let [bookmark    (bdb/create-bookmark #:bookmark{:title "foo" :url "bar" :tab-id @tab1-id :user-id @user-id})
        bookmark-id (:bookmark/id bookmark)
        tab-id      (random-uuid)
        query       [{`(bmut/patch-bookmark #:bookmark{:id ~bookmark-id :tab-id ~tab-id}) bookmark-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        bookmark    (get result `bmut/patch-bookmark)]
    (expect {:error         true
             :error-message "Nonexistent tab"
             :error-type    :invalid-input
             :error-data    {:bookmark/tab-id ["nonexistent"]}} bookmark)))

(defexpect ^:mutation ^:bookmark ^:integration normal-delete-bookmark
  (let [bookmark         (bdb/create-bookmark #:bookmark{:title "foo" :url "bar" :user-id @user-id :tab-id @tab1-id})
        bookmark-id      (:bookmark/id bookmark)
        query            [{`(bmut/delete-bookmark {:bookmark/id ~bookmark-id}) bookmark-join}]
        result           (protected-parser {:request {:user/id @user-id}} query)
        deleted-bookmark (get result `bmut/delete-bookmark)
        fetched-bookmark (bdb/fetch-bookmark {:bookmark/id bookmark-id :bookmark/user-id @user-id})]
    (expect nil fetched-bookmark)
    (expect (:bookmark/id deleted-bookmark) (:bookmark/id bookmark))
    (expect (:bookmark/title deleted-bookmark) (:bookmark/title bookmark))
    (expect (:bookmark/url deleted-bookmark) (:bookmark/url bookmark))
    (expect (:bookmark/image deleted-bookmark) (:bookmark/image bookmark))
    (expect (:bookmark/created deleted-bookmark) (:bookmark/created bookmark))
    (expect (:bookmark/updated deleted-bookmark) (:bookmark/updated bookmark))))

(defexpect ^:mutation ^:bookmark ^:integration normal-delete-nonexistent-tab
  (let [bookmark-id (random-uuid)
        tab-id      "foo"
        query       [{`(bmut/delete-bookmark {:bookmark/id ~bookmark-id}) bookmark-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        bookmark    (get result `bmut/patch-bookmark)]
    (expect nil bookmark)))

(defexpect ^:mutation ^:bookmark ^:integration fail-delete-tab-with-invalid-id
  (let [bookmark-id "foo"
        query       [{`(bmut/delete-bookmark {:bookmark/id ~bookmark-id}) bookmark-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        bookmark    (get result `bmut/delete-bookmark)]
    (expect {:error         true
             :error-message "Invalid input"
             :error-type    :invalid-input
             :error-data    {:bookmark/id ["should be a uuid"]}} bookmark)))

;; FETCH

(defexpect ^:mutation ^:bookmark ^:integration normal-fetch-bookmarks-with-password
  (let [tab-password "bar"
        tab          (-> {:tab/name "foo" :tab/password tab-password :tab/user-id @user-id}
                         (update :tab/password hashers/derive)
                         tab-db/create-tab)
        tab-id       (:tab/id tab)
        bookmark1    (bdb/create-bookmark #:bookmark{:title "foo" :url "bar" :tab-id tab-id :user-id @user-id})
        bookmark2    (bdb/create-bookmark #:bookmark{:title "fim" :url "baz" :tab-id tab-id :user-id @user-id})
        query        [{`(bmut/fetch-bookmarks #:tab{:id ~tab-id :tab/password ~tab-password})
                       [:tab/id :tab/bookmarks]}]
        actual       (protected-parser {:request {:user/id @user-id}} query)
        expected     {`bmut/fetch-bookmarks {:tab/id        tab-id
                                             :tab/bookmarks [(trim-bookmark bookmark1)
                                                             (trim-bookmark bookmark2)]}}]
    (expect expected actual)))

(defexpect ^:mutation ^:bookmark ^:integration normal-fetch-bookmarks-without-password
  (let [tab-password "bar"
        tab          (-> {:tab/name "foo" :tab/user-id @user-id} tab-db/create-tab)
        tab-id       (:tab/id tab)
        bookmark1    (bdb/create-bookmark #:bookmark{:title "foo" :url "bar" :tab-id tab-id :user-id @user-id})
        bookmark2    (bdb/create-bookmark #:bookmark{:title "fim" :url "baz" :tab-id tab-id :user-id @user-id})
        query        [{`(bmut/fetch-bookmarks #:tab{:id ~tab-id}) [:tab/id :tab/bookmarks]}]
        actual       (protected-parser {:request {:user/id @user-id}} query)
        expected     {`bmut/fetch-bookmarks {:tab/id        tab-id
                                             :tab/bookmarks [(trim-bookmark bookmark1)
                                                             (trim-bookmark bookmark2)]}}]
    (expect expected actual)))

(defexpect ^:mutation ^:bookmark ^:integration normal-fetch-unprotected-bookmarks-with-password
  (let [tab-password "bar"
        tab          (-> {:tab/name "foo" :tab/user-id @user-id} tab-db/create-tab)
        tab-id       (:tab/id tab)
        bookmark1    (bdb/create-bookmark #:bookmark{:title "foo" :url "bar" :tab-id tab-id :user-id @user-id})
        bookmark2    (bdb/create-bookmark #:bookmark{:title "fim" :url "baz" :tab-id tab-id :user-id @user-id})
        query        [{`(bmut/fetch-bookmarks #:tab{:id ~tab-id :password "foo"}) [:tab/id :tab/bookmarks]}]
        actual       (protected-parser {:request {:user/id @user-id}} query)
        expected     {`bmut/fetch-bookmarks {:tab/id        tab-id
                                             :tab/bookmarks [(trim-bookmark bookmark1)
                                                             (trim-bookmark bookmark2)]}}]
    (expect expected actual)))

(defexpect ^:mutation ^:bookmark ^:integration fail-to-fetch-bookmarks-with-wrong-password
  (let [tab      (-> {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id}
                     (update :tab/password hashers/derive)
                     tab-db/create-tab)
        tab-id   (:tab/id tab)
        query    [{`(bmut/fetch-bookmarks #:tab{:id ~tab-id :password "baz"}) bookmark-join}]
        actual   (protected-parser {:request {:user/id @user-id}} query)
        expected {`bmut/fetch-bookmarks {:error         true
                                         :error-message "Invalid input"
                                         :error-type    :wrong-password}}]
    (expect expected actual)))

(defexpect ^:mutation ^:bookmark ^:integration fail-to-fetch-bookmarks-with-no-password
  (let [tab      (-> {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id}
                     (update :tab/password hashers/derive)
                     tab-db/create-tab)
        tab-id   (:tab/id tab)
        query    [{`(bmut/fetch-bookmarks #:tab{:id ~tab-id}) bookmark-join}]
        actual   (protected-parser {:request {:user/id @user-id}} query)
        expected {`bmut/fetch-bookmarks {:error         true
                                         :error-message "Invalid input"
                                         :error-type    :wrong-password}}]
    (expect expected actual)))

(defexpect ^:mutation ^:bookmark ^:integration fail-to-fetch-bookmarks-with-empty-password
  (let [tab      (-> {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id}
                     (update :tab/password hashers/derive)
                     tab-db/create-tab)
        tab-id   (:tab/id tab)
        query    [{`(bmut/fetch-bookmarks {:tab/id ~tab-id :tab/password ""}) bookmark-join}]
        actual   (protected-parser {:request {:user/id @user-id}} query)
        expected {`bmut/fetch-bookmarks {:error         true
                                         :error-message "Invalid input"
                                         :error-type    :wrong-password}}]
    (expect expected actual)))

(comment
  (require '[kaocha.repl :as k])
  (k/run 'shinsetsu.mutations.bookmark-test)
  (k/run #'shinsetsu.mutations.bookmark-test/normal-create-bookmark-with-tempid))
