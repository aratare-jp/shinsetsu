(ns shinsetsu.mutations.tab-test
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [shinsetsu.parser :refer [protected-parser]]
    [shinsetsu.mutations.tab :as tab-mut]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.db.tab :as tab-db]
    [taoensso.timbre :as log]))

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

(def tab-join [:tab/id :tab/name :tab/is-protected? :tab/created :tab/updated])

(defn create-tab
  [tab-name tab-password user-id]
  (-> {:tab/name tab-name :tab/password tab-password :tab/user-id user-id}
      (tab-db/create-tab)
      (assoc :tab/is-protected? ((complement nil?) tab-password))
      (dissoc :tab/user-id)
      (dissoc :tab/password)))

(defexpect normal-create-tab
  (let [query    [{`(tab-mut/create-tab {:tab/name "foo" :tab/password "bar"}) tab-join}]
        result   (protected-parser {:request {:user/id @user-id}} query)
        actual   (get result `tab-mut/create-tab)
        tab-id   (:tab/id actual)
        expected (-> (tab-db/fetch-tab {:tab/id tab-id :user/id @user-id})
                     (assoc :tab/is-protected? true)
                     (dissoc :tab/password)
                     (dissoc :tab/user-id))]
    (expect expected actual)))

(defexpect normal-create-tab-without-password
  (let [query    [{`(tab-mut/create-tab {:tab/name "foo"}) tab-join}]
        result   (protected-parser {:request {:user/id @user-id}} query)
        actual   (get result `tab-mut/create-tab)
        tab-id   (:tab/id actual)
        expected (-> (tab-db/fetch-tab {:tab/id tab-id :user/id @user-id})
                     (assoc :tab/is-protected? false)
                     (dissoc :tab/password)
                     (dissoc :tab/user-id))]
    (expect expected actual)))

(defexpect fail-create-tab-without-name
  (let [query    [{`(tab-mut/create-tab {}) tab-join}]
        result   (protected-parser {:request {:user/id @user-id}} query)
        actual   result
        expected {`tab-mut/create-tab {:error         true
                                       :error-message "Invalid tab"
                                       :error-type    :invalid-input
                                       :error-data    {:tab/name ["missing required key"]}}}]
    (expect expected actual)))

(defexpect fail-create-tab-with-invalid-name
  (let [query    [{`(tab-mut/create-tab {:tab/name ""}) tab-join}]
        result   (protected-parser {:request {:user/id @user-id}} query)
        actual   result
        expected {`tab-mut/create-tab {:error         true
                                       :error-message "Invalid tab"
                                       :error-type    :invalid-input
                                       :error-data    {:tab/name ["should be at least 1 characters"]}}}]
    (expect expected actual)))

(defexpect fail-create-tab-with-invalid-password
  (let [query    [{`(tab-mut/create-tab {:tab/name "foo" :tab/password ""}) tab-join}]
        result   (protected-parser {:request {:user/id @user-id}} query)
        actual   result
        expected {`tab-mut/create-tab {:error         true
                                       :error-message "Invalid tab"
                                       :error-type    :invalid-input
                                       :error-data    {:tab/password ["should be at least 1 characters"]}}}]
    (expect expected actual)))

(comment
  (let [a 1]
    '~a)
  `tab-mut/create-tab
  (require '[kaocha.repl :as k])
  (require '[shinsetsu.mutations.tab-test])
  `(tab-mut/create-tab {:tab/name "foo"})
  '(~`tab-mut/create-tab {:tab 1})
  (k/run 'shinsetsu.mutations.tab-test)
  (k/run #'shinsetsu.mutations.tab-test/normal-create-tab-without-password))
