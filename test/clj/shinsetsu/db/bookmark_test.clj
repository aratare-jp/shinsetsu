(ns shinsetsu.db.bookmark-test
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.db.tab :as tab-db]
    [shinsetsu.db.bookmark :as bdb]
    [shinsetsu.db.tag :as tdb]
    [shinsetsu.db.bookmark-tag :as btdb]
    [shinsetsu.schema :as s]
    [malli.core :as m]
    [malli.error :as em]
    [malli.error :as me]
    [taoensso.timbre :as log]
    [shinsetsu.db.bookmark-tag :as bookmark-tag-db])
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

(defexpect normal-simplify-query
  (let [expected [{:must {:tag {:and ["ent"]}} :must-not {}}
                  {:must {:tag {:and ["wish list"]}} :must-not {}}
                  {:must {:tag {:and ["gam" "ent"]}} :must-not {}}
                  {:must {:tag {:and ["online store" "wish list"]}} :must-not {}}
                  {:must {:tag {:and ["ent" "wish list"]}} :must-not {}}
                  {:must {:tag {:or ["news" "gam"]}} :must-not {}}
                  {:must {:tag {:or ["read later" "wish list"]}} :must-not {}}]
        actual   [{:bool {:must [{:match {:tag {:query "ent" :operator "and"}}}]}}
                  {:bool {:must [{:match_phrase {:tag "wish list"}}]}}
                  {:bool {:must [{:match {:tag {:query "ent gam", :operator "and"}}}]}}
                  {:bool {:must [{:bool {:must [{:match_phrase {:tag "wish list"}}
                                                {:match_phrase {:tag "online store"}}]}}]}}
                  {:bool {:must [{:bool {:must [{:match_phrase {:tag "wish list"}}
                                                {:match {:tag {:query "ent" :operator "and"}}}]}}]}}
                  {:bool {:must [{:match {:tag {:query "gam news", :operator "or"}}}]}}
                  {:bool {:must [{:bool {:should [{:match_phrase {:tag "wish list"}}
                                                  {:match_phrase {:tag "read later"}}]}}]}}]]
    (doall
      (for [i (vec (range (count expected)))]
        (expect (nth expected i) (bdb/simplify-query (nth actual i)))))))

(defexpect normal-create-bookmark
  (let [bookmark-title "hello"
        bookmark-url   "world"
        bookmark       (bdb/create-bookmark {:bookmark/title   bookmark-title
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
    (bdb/create-bookmark {:bookmark/url     "foo"
                          :bookmark/tab-id  @tab1-id
                          :bookmark/user-id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/title ["missing required key"]}} data)))))

(defexpect fail-create-bookmark-with-invalid-title
  (try
    (bdb/create-bookmark {:bookmark/title   ""
                          :bookmark/url     "foo"
                          :bookmark/tab-id  @tab1-id
                          :bookmark/user-id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/title ["should be at least 1 characters"]}} data)))))

(defexpect fail-create-bookmark-without-url
  (try
    (bdb/create-bookmark {:bookmark/title   "foo"
                          :bookmark/tab-id  @tab1-id
                          :bookmark/user-id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/url ["missing required key"]}} data)))))

(defexpect fail-create-bookmark-with-invalid-url
  (try
    (bdb/create-bookmark {:bookmark/title   "foo"
                          :bookmark/url     ""
                          :bookmark/tab-id  @tab1-id
                          :bookmark/user-id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/url ["should be at least 1 characters"]}} data)))))

(defexpect fail-create-bookmark-without-tab
  (try
    (bdb/create-bookmark {:bookmark/title   "foo"
                          :bookmark/url     "bar"
                          :bookmark/user-id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/tab-id ["missing required key"]}} data)))))

(defexpect fail-create-bookmark-with-invalid-tab
  (try
    (bdb/create-bookmark {:bookmark/title   "foo"
                          :bookmark/url     "bar"
                          :bookmark/tab-id  "foo"
                          :bookmark/user-id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/tab-id ["should be a uuid"]}} data)))))

(defexpect fail-create-bookmark-without-user
  (try
    (bdb/create-bookmark {:bookmark/title  "foo"
                          :bookmark/url    "bar"
                          :bookmark/tab-id @tab1-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/user-id ["missing required key"]}} data)))))

(defexpect fail-create-bookmark-with-invalid-user
  (try
    (bdb/create-bookmark {:bookmark/title   "foo"
                          :bookmark/url     "bar"
                          :bookmark/user-id @user-id
                          :bookmark/tab-id  "foo"})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/tab-id ["should be a uuid"]}} data)))))

(defexpect normal-patch-bookmark-with-new-title-and-url-and-tab
  (let [bookmark-title     "hello"
        bookmark-url       "world"
        new-bookmark-title "foo"
        new-bookmark-url   "bar"
        bookmark           (bdb/create-bookmark {:bookmark/title   bookmark-title
                                                 :bookmark/url     bookmark-url
                                                 :bookmark/tab-id  @tab1-id
                                                 :bookmark/user-id @user-id})
        bookmark-id        (:bookmark/id bookmark)
        patched-bookmark   (bdb/patch-bookmark {:bookmark/id      bookmark-id
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
        bookmark           (bdb/create-bookmark {:bookmark/title   bookmark-title
                                                 :bookmark/url     bookmark-url
                                                 :bookmark/tab-id  @tab1-id
                                                 :bookmark/user-id @user-id})
        bookmark-id        (:bookmark/id bookmark)
        patched-bookmark   (bdb/patch-bookmark {:bookmark/id      bookmark-id
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
        bookmark         (bdb/create-bookmark {:bookmark/title   bookmark-title
                                               :bookmark/url     bookmark-url
                                               :bookmark/tab-id  @tab1-id
                                               :bookmark/user-id @user-id})
        bookmark-id      (:bookmark/id bookmark)
        patched-bookmark (bdb/patch-bookmark {:bookmark/id      bookmark-id
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
        bookmark         (bdb/create-bookmark {:bookmark/title   bookmark-title
                                               :bookmark/url     bookmark-url
                                               :bookmark/tab-id  @tab1-id
                                               :bookmark/user-id @user-id})
        bookmark-id      (:bookmark/id bookmark)
        patched-bookmark (bdb/patch-bookmark {:bookmark/id      bookmark-id
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
          bookmark           (bdb/create-bookmark {:bookmark/title   bookmark-title
                                                   :bookmark/url     bookmark-url
                                                   :bookmark/tab-id  @tab1-id
                                                   :bookmark/user-id @user-id})
          bookmark-id        (:bookmark/id bookmark)]
      (bdb/patch-bookmark {:bookmark/id      bookmark-id
                           :bookmark/user-id @user-id
                           :bookmark/title   new-bookmark-title})
      (expect false))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/title ["should be at least 1 characters"]}} data)))))

(defexpect fail-patch-bookmark-with-invalid-url
  (try
    (let [bookmark-title   "foo"
          bookmark-url     "bar"
          new-bookmark-url ""
          bookmark         (bdb/create-bookmark {:bookmark/title   bookmark-title
                                                 :bookmark/url     bookmark-url
                                                 :bookmark/tab-id  @tab1-id
                                                 :bookmark/user-id @user-id})
          bookmark-id      (:bookmark/id bookmark)]
      (bdb/patch-bookmark {:bookmark/id      bookmark-id
                           :bookmark/user-id @user-id
                           :bookmark/url     new-bookmark-url})
      (expect false))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/url ["should be at least 1 characters"]}} data)))))

(defexpect fail-patch-bookmark-with-invalid-tab
  (try
    (let [bookmark-title "foo"
          bookmark-url   "bar"
          new-tab-id     "boo"
          bookmark       (bdb/create-bookmark {:bookmark/title   bookmark-title
                                               :bookmark/url     bookmark-url
                                               :bookmark/tab-id  @tab1-id
                                               :bookmark/user-id @user-id})
          bookmark-id    (:bookmark/id bookmark)]
      (bdb/patch-bookmark {:bookmark/id      bookmark-id
                           :bookmark/user-id @user-id
                           :bookmark/tab-id  new-tab-id})
      (expect false))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/tab-id ["should be a uuid"]}} data)))))

(defexpect fail-patch-bookmark-with-nonexistent-tab
  (try
    (let [bookmark-title "foo"
          bookmark-url   "bar"
          new-tab-id     (random-uuid)
          bookmark       (bdb/create-bookmark {:bookmark/title   bookmark-title
                                               :bookmark/url     bookmark-url
                                               :bookmark/tab-id  @tab1-id
                                               :bookmark/user-id @user-id})
          bookmark-id    (:bookmark/id bookmark)]
      (bdb/patch-bookmark {:bookmark/id      bookmark-id
                           :bookmark/user-id @user-id
                           :bookmark/tab-id  new-tab-id})
      (expect false))
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Nonexistent tab" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/tab-id ["nonexistent"]}} data)))))

(defexpect normal-delete-bookmark
  (let [bookmark         (bdb/create-bookmark {:bookmark/title   "foo"
                                               :bookmark/url     "bar"
                                               :bookmark/tab-id  @tab1-id
                                               :bookmark/user-id @user-id})
        bookmark-id      (:bookmark/id bookmark)
        deleted-bookmark (bdb/delete-bookmark {:bookmark/id bookmark-id :bookmark/user-id @user-id})
        fetched-bookmark (bdb/fetch-bookmark {:bookmark/id bookmark-id :bookmark/user-id @user-id})]
    (expect nil fetched-bookmark)
    (expect (:bookmark/id bookmark) (:bookmark/id deleted-bookmark))
    (expect (:bookmark/title bookmark) (:bookmark/title deleted-bookmark))
    (expect (:bookmark/url bookmark) (:bookmark/url deleted-bookmark))
    (expect (:bookmark/created bookmark) (:bookmark/created deleted-bookmark))
    (expect (:bookmark/updated bookmark) (:bookmark/updated deleted-bookmark))))

(defexpect normal-delete-nonexistent-bookmark
  (let [deleted-bookmark (bdb/delete-bookmark {:bookmark/id (random-uuid) :bookmark/user-id @user-id})]
    (expect nil deleted-bookmark)))

(defexpect fail-delete-bookmark-with-invalid-id
  (try
    (bdb/delete-bookmark {:bookmark/id "foo" :bookmark/user-id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/id ["should be a uuid"]}} data)))))

(comment
  (require '[kaocha.repl :as k])
  (k/run #'shinsetsu.db.bookmark-test/fail-delete-bookmark-with-invalid-id))

(defexpect normal-fetch-bookmark
  (let [bookmark    (bdb/create-bookmark #:bookmark{:title "foo" :url "bar" :tab-id @tab1-id :user-id @user-id})
        bookmark-id (:bookmark/id bookmark)
        fetched     (bdb/fetch-bookmark #:bookmark{:id bookmark-id :user-id @user-id})]
    (expect bookmark fetched)))

(defexpect normal-fetch-nonexistent-bookmark nil (bdb/fetch-bookmark {:bookmark/id      (random-uuid)
                                                                      :bookmark/user-id @user-id}))

(defexpect fail-fetch-bookmark-without-id
  (try
    (bdb/fetch-bookmark {:bookmark/user-id @user-id})
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/id ["missing required key"]}} data)))))

(defexpect fail-fetch-bookmark-with-invalid-id
  (try
    (bdb/fetch-bookmark {:bookmark/id "foo" :bookmark/user-id @user-id})
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/id ["should be a uuid"]}} data)))))

(defexpect fail-fetch-bookmark-without-user-id
  (try
    (bdb/fetch-bookmark {:bookmark/id (random-uuid)})
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/user-id ["missing required key"]}} data)))))

(defexpect fail-fetch-bookmark-with-invalid-user-id
  (try
    (bdb/fetch-bookmark {:bookmark/id (random-uuid) :bookmark/user-id "foo"})
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/user-id ["should be a uuid"]}} data)))))

(defexpect normal-fetch-bookmarks
  (let [bookmark1-title   "hello"
        bookmark2-title   "hello"
        bookmark1-url     "world"
        bookmark2-url     "world"
        bookmark1         (bdb/create-bookmark {:bookmark/title   bookmark1-title
                                                :bookmark/url     bookmark1-url
                                                :bookmark/tab-id  @tab1-id
                                                :bookmark/user-id @user-id})
        bookmark2         (bdb/create-bookmark {:bookmark/title   bookmark2-title
                                                :bookmark/url     bookmark2-url
                                                :bookmark/tab-id  @tab1-id
                                                :bookmark/user-id @user-id})
        fetched-bookmarks (bdb/fetch-bookmarks {:bookmark/user-id @user-id :bookmark/tab-id @tab1-id})]
    (expect [bookmark1 bookmark2] fetched-bookmarks)))

(defexpect normal-fetch-bookmarks-simple-title-query
  (let [bookmark1 (bdb/create-bookmark #:bookmark{:title "hello" :url "world" :tab-id @tab1-id :user-id @user-id})
        bookmark2 (bdb/create-bookmark #:bookmark{:title "cool" :url "hot" :tab-id @tab1-id :user-id @user-id})
        expected1 [bookmark1]
        actual1   (bdb/fetch-bookmarks {:bookmark/user-id @user-id :bookmark/tab-id @tab1-id}
                                       {:query {:bool {:must [{:simple_query_string {:query "hel"}}]}}})
        expected2 [bookmark1 bookmark2]
        actual2   (bdb/fetch-bookmarks {:bookmark/user-id @user-id :bookmark/tab-id @tab1-id}
                                       {:query {:bool {:must [{:simple_query_string {:query "l"}}]}}})
        expected3 []
        actual3   (bdb/fetch-bookmarks {:bookmark/user-id @user-id :bookmark/tab-id @tab1-id}
                                       {:query {:bool {:must [{:simple_query_string {:query "z"}}]}}})]
    (expect expected1 actual1)
    (expect expected2 actual2)
    (expect expected3 actual3)))

(defexpect normal-fetch-bookmarks-simple-tag-query
  (let [bookmark1 (bdb/create-bookmark #:bookmark{:title "twitch" :url "foo" :tab-id @tab1-id :user-id @user-id})
        bid1      (:bookmark/id bookmark1)
        bookmark2 (bdb/create-bookmark #:bookmark{:title "youtube" :url "foo" :tab-id @tab1-id :user-id @user-id})
        bid2      (:bookmark/id bookmark2)
        bookmark3 (bdb/create-bookmark #:bookmark{:title "steam" :url "foo" :tab-id @tab1-id :user-id @user-id})
        bid3      (:bookmark/id bookmark3)
        bookmark4 (bdb/create-bookmark #:bookmark{:title "feedly" :url "foo" :tab-id @tab1-id :user-id @user-id})
        bid4      (:bookmark/id bookmark4)
        tag1      (tdb/create-tag #:tag{:name "entertainment" :user-id @user-id})
        tid1      (:tag/id tag1)
        tag2      (tdb/create-tag #:tag{:name "gaming" :user-id @user-id})
        tid2      (:tag/id tag2)
        tag3      (tdb/create-tag #:tag{:name "video" :user-id @user-id})
        tid3      (:tag/id tag3)
        tag4      (tdb/create-tag #:tag{:name "news" :user-id @user-id})
        tid4      (:tag/id tag4)
        tag5      (tdb/create-tag #:tag{:name "wish list" :user-id @user-id})
        tid5      (:tag/id tag5)
        tag6      (tdb/create-tag #:tag{:name "read later" :user-id @user-id})
        tid6      (:tag/id tag6)
        tag7      (tdb/create-tag #:tag{:name "online store" :user-id @user-id})
        tid7      (:tag/id tag7)
        _         (do
                    (btdb/create-bookmark-tags #:bookmark-tag{:bookmark-id bid1 :user-id @user-id :tag-ids [tid1 tid2 tid3]})
                    (btdb/create-bookmark-tags #:bookmark-tag{:bookmark-id bid2 :user-id @user-id :tag-ids [tid1 tid3]})
                    (btdb/create-bookmark-tags #:bookmark-tag{:bookmark-id bid3 :user-id @user-id :tag-ids [tid1 tid2 tid5 tid7]})
                    (btdb/create-bookmark-tags #:bookmark-tag{:bookmark-id bid4 :user-id @user-id :tag-ids [tid3 tid4 tid6]}))
        ;; tag:foo
        ;; tag:"foo"
        expected1 [bookmark1 bookmark2 bookmark3]
        actual1   (bdb/fetch-bookmarks {:bookmark/user-id @user-id :bookmark/tab-id @tab1-id}
                                       {:query {:bool {:must [{:match {:tag {:query "ent" :operator "and"}}}]}}})
        ;; tag:"wish list"
        expected2 [bookmark3 bookmark4]
        actual2   (bdb/fetch-bookmarks {:bookmark/user-id @user-id :bookmark/tab-id @tab1-id}
                                       {:query {:bool {:must [{:match_phrase {:tag "wish list"}}]}}})
        ;; tag:ent tag:gam
        ;; tag:"ent" tag:"gam"
        expected3 [bookmark1 bookmark3]
        actual3   (bdb/fetch-bookmarks {:bookmark/user-id @user-id :bookmark/tab-id @tab1-id}
                                       {:query {:bool {:must [{:match {:tag {:query "ent gam", :operator "and"}}}]}}})
        ;; tag:"wish list" tag:"online store"
        expected4 [bookmark3]
        actual4   (bdb/fetch-bookmarks {:bookmark/user-id @user-id :bookmark/tab-id @tab1-id}
                                       {:query {:bool {:must [{:bool {:must [{:match_phrase {:tag "wish list"}}
                                                                             {:match_phrase {:tag "online store"}}]}}]}}})
        ;; tag:"wish list" tag:ent
        expected5 [bookmark3]
        actual5   (bdb/fetch-bookmarks {:bookmark/user-id @user-id :bookmark/tab-id @tab1-id}
                                       {:query {:bool {:must [{:bool {:must [{:match_phrase {:tag "wish list"}}
                                                                             {:match {:tag {:query "ent" :operator "and"}}}]}}]}}})
        ;; tag:(gam OR new)
        ;; tag:("gam" OR "new")
        expected6 [bookmark1 bookmark3 bookmark4]
        actual6   (bdb/fetch-bookmarks {:bookmark/user-id @user-id :bookmark/tab-id @tab1-id}
                                       {:query {:bool {:must [{:match {:tag {:query "gam news", :operator "or"}}}]}}})
        ;; tag:("wish list" OR "read later")
        expected7 [bookmark3 bookmark4]
        actual7   (bdb/fetch-bookmarks {:bookmark/user-id @user-id :bookmark/tab-id @tab1-id}
                                       {:query {:bool {:must [{:bool {:should [{:match_phrase {:tag "wish list"}}
                                                                               {:match_phrase {:tag "read later"}}]}}]}}})]
    (expect expected1 actual1)
    (expect expected2 actual2)
    (expect expected3 actual3)
    (expect expected4 actual4)
    (expect expected5 actual5)
    (expect expected6 actual6)
    (expect expected7 actual7)))

(comment
  (k/run #'shinsetsu.db.bookmark-test/normal-fetch-bookmarks-simple-tag-query))

(defexpect normal-fetch-bookmarks-sort-by-created-desc
  (let [bookmark1-title   "hello"
        bookmark2-title   "hello"
        bookmark1-url     "world"
        bookmark2-url     "world"
        bookmark1         (bdb/create-bookmark {:bookmark/title   bookmark1-title
                                                :bookmark/url     bookmark1-url
                                                :bookmark/tab-id  @tab1-id
                                                :bookmark/user-id @user-id})
        bookmark2         (bdb/create-bookmark {:bookmark/title   bookmark2-title
                                                :bookmark/url     bookmark2-url
                                                :bookmark/tab-id  @tab1-id
                                                :bookmark/user-id @user-id})
        fetched-bookmarks (bdb/fetch-bookmarks {:bookmark/user-id @user-id :bookmark/tab-id @tab1-id}
                                               {:sort {:field :bookmark/created :direction :desc}})]
    (expect [bookmark2 bookmark1] fetched-bookmarks)))

(defexpect fetch-empty-bookmarks [] (bdb/fetch-bookmarks {:bookmark/tab-id @tab1-id :bookmark/user-id @user-id}))

(defexpect fail-fetch-bookmarks-without-user-and-tab
  (try
    (bdb/fetch-bookmarks {})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect
          {:error-type :invalid-input
           :error-data {:bookmark/tab-id  ["missing required key"]
                        :bookmark/user-id ["missing required key"]}}
          data)))))

(defexpect fail-fetch-bookmarks-without-user
  (try
    (bdb/fetch-bookmarks {:bookmark/tab-id @tab1-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/user-id ["missing required key"]}} data)))))

(defexpect fail-fetch-bookmarks-with-invalid-user
  (try
    (bdb/fetch-bookmarks {:bookmark/tab-id @tab1-id :bookmark/user-id "foo"})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/user-id ["should be a uuid"]}} data)))))

(defexpect fail-fetch-bookmarks-without-tab
  (try
    (bdb/fetch-bookmarks {:bookmark/user-id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/tab-id ["missing required key"]}} data)))))

(defexpect fail-fetch-bookmarks-with-invalid-tab
  (try
    (bdb/fetch-bookmarks {:bookmark/tab-id "foo" :bookmark/user-id @user-id})
    (expect false)
    (catch Exception e
      (let [data    (ex-data e)
            message (ex-message e)]
        (expect "Invalid input" message)
        (expect {:error-type :invalid-input :error-data {:bookmark/tab-id ["should be a uuid"]}} data)))))

(comment
  (require '[kaocha.repl :as k])
  (k/run 'shinsetsu.db.bookmark-test)
  (k/run #'shinsetsu.db.bookmark-test/fail-fetch-tags-by-invalid-bookmark))
