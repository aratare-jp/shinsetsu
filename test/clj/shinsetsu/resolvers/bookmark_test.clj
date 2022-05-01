(ns shinsetsu.resolvers.bookmark-test
  (:require
    [buddy.hashers :as hashers]
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.db.bookmark :as bookmark-db]
    [shinsetsu.db.tab :as tab-db]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.parser :refer [protected-parser]]
    [shinsetsu.test-utility :refer [db-cleanup db-setup]])
  (:import [java.time Duration Instant]))

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

(def bookmark-join [:bookmark/id :bookmark/title :bookmark/url :bookmark/image :bookmark/favourite :bookmark/created :bookmark/updated])
(defn trim-bookmark [b] (dissoc b :bookmark/user-id :bookmark/tab-id))

(defn create-bookmark
  [bookmark-title bookmark-url tab-id user-id]
  (-> {:bookmark/title   bookmark-title
       :bookmark/url     bookmark-url
       :bookmark/tab-id  tab-id
       :bookmark/user-id user-id}
      (bookmark-db/create-bookmark)
      (dissoc :bookmark/tab-id)
      (dissoc :bookmark/user-id)))

(defexpect normal-fetch-bookmark
  (let [bookmark       (create-bookmark "foo" "bar" @tab-id @user-id)
        bookmark-id    (:bookmark/id bookmark)
        fetch-bookmark (protected-parser {:request {:user/id @user-id}} [{[:bookmark/id bookmark-id] bookmark-join}])]
    (expect {[:bookmark/id bookmark-id] bookmark} fetch-bookmark)))

(defexpect normal-fetch-empty-bookmark
  (let [random-id (random-uuid)
        expected  {[:bookmark/id random-id] {:bookmark/id random-id}}
        actual    (protected-parser {:request {:user/id @user-id}} [{[:bookmark/id random-id] bookmark-join}])]
    (expect expected actual)))

(defexpect fail-fetch-invalid-bookmark
  (let [random-id "foo"
        expected  {[:bookmark/id random-id] {:error         true
                                             :error-type    :invalid-input
                                             :error-message "Invalid input"
                                             :error-data    {:bookmark/id ["should be a uuid"]}}}
        actual    (protected-parser {:request {:user/id @user-id}} [{[:bookmark/id random-id] bookmark-join}])]
    (expect expected actual)))

(defexpect fail-fetch-null-bookmark
  (let [random-id nil
        expected  {[:bookmark/id random-id] {:error         true
                                             :error-type    :invalid-input
                                             :error-message "Invalid input"
                                             :error-data    {:bookmark/id ["should be a uuid"]}}}
        actual    (protected-parser {:request {:user/id @user-id}} [{[:bookmark/id random-id] bookmark-join}])]
    (expect expected actual)))

(defexpect ^:resolver ^:bookmark ^:integration normal-fetch-bookmarks-with-password
  (let [tab-password "bar"
        tab          (-> {:tab/name "foo" :tab/password tab-password :tab/user-id @user-id}
                         (update :tab/password hashers/derive)
                         tab-db/create-tab)
        tab-id       (:tab/id tab)
        bookmark1    (bookmark-db/create-bookmark #:bookmark{:title "foo" :url "bar" :tab-id tab-id :user-id @user-id})
        bookmark2    (bookmark-db/create-bookmark #:bookmark{:title "fim" :url "baz" :tab-id tab-id :user-id @user-id})
        query        [`({[:tab/id ~tab-id] [:tab/id :tab/bookmarks]} {:tab/password ~tab-password})]
        actual       (protected-parser {:request {:user/id @user-id}} query)
        expected     {[:tab/id tab-id] {:tab/id        tab-id
                                        :tab/bookmarks [(trim-bookmark bookmark1)
                                                        (trim-bookmark bookmark2)]}}]
    (expect expected actual)))

(defexpect ^:resolver ^:bookmark ^:integration normal-fetch-bookmarks-without-password
  (let [tab       (tab-db/create-tab {:tab/name "foo" :tab/user-id @user-id})
        tab-id    (:tab/id tab)
        bookmark1 (bookmark-db/create-bookmark #:bookmark{:title "foo" :url "bar" :tab-id tab-id :user-id @user-id})
        bookmark2 (bookmark-db/create-bookmark #:bookmark{:title "fim" :url "baz" :tab-id tab-id :user-id @user-id})
        query     [{[:tab/id tab-id] [:tab/id :tab/bookmarks]}]
        actual    (protected-parser {:request {:user/id @user-id}} query)
        expected  {[:tab/id tab-id] {:tab/id        tab-id
                                     :tab/bookmarks [(trim-bookmark bookmark1)
                                                     (trim-bookmark bookmark2)]}}]
    (expect expected actual)))

(defexpect ^:resolver ^:bookmark ^:integration normal-fetch-unprotected-bookmarks-without-password
  (let [tab-password "bar"
        tab          (tab-db/create-tab {:tab/name "foo" :tab/user-id @user-id})
        tab-id       (:tab/id tab)
        bookmark1    (bookmark-db/create-bookmark #:bookmark{:title "foo" :url "bar" :tab-id tab-id :user-id @user-id})
        bookmark2    (bookmark-db/create-bookmark #:bookmark{:title "fim" :url "baz" :tab-id tab-id :user-id @user-id})
        query        [`({[:tab/id ~tab-id] [:tab/id :tab/bookmarks]} {:tab/password ~tab-password})]
        actual       (protected-parser {:request {:user/id @user-id}} query)
        expected     {[:tab/id tab-id] {:tab/id        tab-id
                                        :tab/bookmarks [(trim-bookmark bookmark1)
                                                        (trim-bookmark bookmark2)]}}]
    (expect expected actual)))

(defexpect ^:resolver ^:bookmark ^:integration normal-fetch-protected-bookmarks-previous-unlocked
  (let [tab-password "bar"
        tab-id       (-> {:tab/name "foo" :tab/password tab-password :tab/user-id @user-id}
                         (update :tab/password hashers/derive)
                         tab-db/create-tab
                         :tab/id)
        bookmark1    (bookmark-db/create-bookmark #:bookmark{:title "foo" :url "bar" :tab-id tab-id :user-id @user-id})
        bookmark2    (bookmark-db/create-bookmark #:bookmark{:title "fim" :url "baz" :tab-id tab-id :user-id @user-id})
        query        [{[:tab/id tab-id] [:tab/id :tab/bookmarks]}]
        actual       (protected-parser {:request {:user/id @user-id}} query)
        expected     {[:tab/id tab-id] {:tab/id        tab-id
                                        :tab/bookmarks [(trim-bookmark bookmark1)
                                                        (trim-bookmark bookmark2)]}}]
    (expect expected actual)))

(defexpect ^:resolver ^:bookmark ^:integration fail-to-fetch-bookmarks-with-wrong-password
  (let [tab-password   "bar"
        wrong-password "foo"
        tab-id         (-> {:tab/name "foo" :tab/password tab-password :tab/user-id @user-id}
                           (update :tab/password hashers/derive)
                           tab-db/create-tab
                           :tab/id)
        _              (tab-db/patch-tab #:tab{:id      tab-id
                                               :user-id @user-id
                                               :unlock  (.minus (Instant/now) (Duration/ofDays 10))})
        query          [`({[:tab/id ~tab-id] [:tab/id :tab/bookmarks]} {:tab/password ~wrong-password})]
        actual         (protected-parser {:request {:user/id @user-id}} query)
        expected       {[:tab/id tab-id] {:error         true
                                          :error-type    :wrong-password
                                          :error-message "Invalid input"}}]
    (expect expected actual)))

(defexpect ^:resolver ^:bookmark ^:integration fail-to-fetch-protected-bookmarks-with-no-password
  (let [tab-password "bar"
        tab-id       (-> {:tab/name "foo" :tab/password tab-password :tab/user-id @user-id}
                         (update :tab/password hashers/derive)
                         tab-db/create-tab
                         :tab/id)
        _            (tab-db/patch-tab #:tab{:id      tab-id
                                             :user-id @user-id
                                             :unlock  (.minus (Instant/now) (Duration/ofDays 10))})
        query        [{[:tab/id tab-id] [:tab/id :tab/bookmarks]}]
        actual       (protected-parser {:request {:user/id @user-id}} query)
        expected     {[:tab/id tab-id] {:error         true
                                        :error-type    :wrong-password
                                        :error-message "Invalid input"}}]
    (expect expected actual)))

(defexpect ^:resolver ^:bookmark ^:integration fail-to-fetch-protected-bookmarks-with-empty-password
  (let [tab-password "bar"
        tab-id       (-> {:tab/name "foo" :tab/password tab-password :tab/user-id @user-id}
                         (update :tab/password hashers/derive)
                         tab-db/create-tab
                         :tab/id)
        _            (tab-db/patch-tab #:tab{:id      tab-id
                                             :user-id @user-id
                                             :unlock  (.minus (Instant/now) (Duration/ofDays 10))})
        query        [`({[:tab/id ~tab-id] [:tab/id :tab/bookmarks]} {:tab/password ""})]
        actual       (protected-parser {:request {:user/id @user-id}} query)
        expected     {[:tab/id tab-id] {:error         true
                                        :error-type    :wrong-password
                                        :error-message "Invalid input"}}]
    (expect expected actual)))

(comment
  (require '[kaocha.repl :as k])
  (require '[shinsetsu.parser :refer [protected-parser]])
  (k/run 'shinsetsu.resolvers.bookmark-test))
