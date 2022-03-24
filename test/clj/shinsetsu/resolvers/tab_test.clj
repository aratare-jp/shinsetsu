(ns shinsetsu.resolvers.tab-test
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.db.tab :as tab-db]
    [shinsetsu.parser :refer [protected-parser]]
    [taoensso.timbre :as log]
    [com.wsscode.pathom.core :as pc]
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

(def tab-join [:tab/id :tab/name :tab/is-protected? :tab/password :tab/created :tab/updated])

(defn create-tab
  [t]
  (let [is-protected (= "" (:tab/password t))]
    (-> t
        (update :tab/password hashers/derive)
        (tab-db/create-tab)
        (assoc :tab/is-protected? is-protected)
        (dissoc :tab/user-id)
        (dissoc :tab/password))))

(defexpect normal-fetch-tab
  (let [tab         (create-tab {:tab/name "foo" :tab/password "foo" :tab/user-id @user-id})
        tab-id      (:tab/id tab)
        fetched-tab (protected-parser {:request {:user/id @user-id}} [{[:tab/id tab-id] tab-join}])]
    (expect {[:tab/id tab-id] tab} fetched-tab)))

(defexpect normal-fetch-empty-tab
  (let [random-id (UUID/randomUUID)
        expected  {[:tab/id random-id] {:tab/id random-id}}
        actual    (protected-parser {:request {:user/id @user-id}} [{[:tab/id random-id] tab-join}])]
    (expect expected actual)))

(defexpect fail-fetch-invalid-tab
  (let [random-id   "foo"
        inner-error {:error         true
                     :error-type    :invalid-input
                     :error-message "Invalid user or tab ID"
                     :error-data    {:tab/id ["should be a uuid"]}}
        expected    {[:tab/id random-id] {:tab/id            random-id
                                          :tab/name          ::pc/reader-error
                                          :tab/is-protected? ::pc/reader-error
                                          :tab/created       ::pc/reader-error
                                          :tab/updated       ::pc/reader-error}
                     ::pc/errors         {[[:tab/id random-id] :tab/name]          inner-error
                                          [[:tab/id random-id] :tab/is-protected?] inner-error
                                          [[:tab/id random-id] :tab/created]       inner-error
                                          [[:tab/id random-id] :tab/updated]       inner-error}}
        actual      (protected-parser {:request {:user/id @user-id}} [{[:tab/id random-id] tab-join}])]
    (expect expected actual)))

(defexpect normal-fetch-tabs
  (let [tab1     (create-tab {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id})
        tab2     (create-tab {:tab/name "foo1" :tab/password "bar2" :tab/user-id @user-id})
        tab3     (create-tab {:tab/name "foo1" :tab/password "" :tab/user-id @user-id})
        expected {:user/tabs [tab1 tab2 tab3]}
        actual   (protected-parser {:request {:user/id @user-id}} [{:user/tabs tab-join}])]
    (expect expected actual)))

(defexpect normal-fetch-empty-tabs
  (let [expected {:user/tabs []}
        actual   (protected-parser {:request {:user/id @user-id}} [{:user/tabs tab-join}])]
    (expect expected actual)))

(comment
  (require '[kaocha.repl :as k])
  (require '[shinsetsu.parser :refer [protected-parser]])
  (k/run 'shinsetsu.resolvers.tab-test)
  (k/run #'shinsetsu.resolvers.tab-test/normal-fetch-tabs))
