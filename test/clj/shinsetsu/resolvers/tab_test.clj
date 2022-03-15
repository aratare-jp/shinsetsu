(ns shinsetsu.resolvers.tab-test
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.db.tab :as tab-db]
    [shinsetsu.parser :refer [protected-parser]]
    [taoensso.timbre :as log])
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

(def tab-join [:tab/id :tab/name :tab/password :tab/created :tab/updated :tab/is-protected?])

(defn create-tab
  [tab-name tab-password user-id]
  (-> {:tab/name tab-name :tab/password tab-password :tab/user-id user-id}
      (tab-db/create-tab)
      (assoc :tab/is-protected? ((complement nil?) tab-password))
      (dissoc :tab/user-id)
      (dissoc :tab/password)))

(defexpect normal-fetch-tab
  (let [tab         (create-tab "foo" "bar" @user-id)
        tab-id      (:tab/id tab)
        fetched-tab (protected-parser {:request {:user/id @user-id}} [{[:tab/id tab-id] tab-join}])]
    (expect {[:tab/id tab-id] tab} fetched-tab)))

(defexpect fetch-empty-tab
  (let [random-id (UUID/randomUUID)
        expected  {[:tab/id random-id] {:tab/id random-id :tab/is-protected? false}}
        actual    (protected-parser {:request {:user/id @user-id}} [{[:tab/id random-id] tab-join}])]
    (expect expected actual)))

(defexpect normal-fetch-tabs
  (let [tab1     (create-tab "foo" "bar" @user-id)
        tab2     (create-tab "foo" "bar" @user-id)
        expected {:tab/tabs [tab1 tab2]}
        actual   (protected-parser {:request {:user/id @user-id}} [{:tab/tabs tab-join}])]
    (expect expected actual)))

(defexpect fetch-empty-tabs
  (let [expected {:tab/tabs []}
        actual   (protected-parser {:request {:user/id @user-id}} [{:tab/tabs tab-join}])]
    (expect expected actual)))

(comment
  (require '[kaocha.repl :as k])
  (require '[shinsetsu.parser :refer [protected-parser]])
  (k/run 'shinsetsu.resolvers.tab-test)
  (k/run #'shinsetsu.resolvers.tab-test/normal-fetch-tab))
