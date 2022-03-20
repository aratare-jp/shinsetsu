(ns shinsetsu.mutations.tab-test
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [shinsetsu.parser :refer [protected-parser]]
    [shinsetsu.mutations.tab :as tab-mut]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.db.tab :as tab-db]
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

(def tab-join [:tab/id :tab/name :tab/is-protected? :tab/created :tab/updated])

(defexpect normal-create-tab-with-protection
  (let [query    [{`(tab-mut/create-tab {:tab/name "foo" :tab/password "bar" :tab/is-protected? true}) tab-join}]
        result   (protected-parser {:request {:user/id @user-id}} query)
        actual   (get result `tab-mut/create-tab)
        tab-id   (:tab/id actual)
        expected (-> (tab-db/fetch-tab {:tab/id tab-id :user/id @user-id})
                     (assoc :tab/is-protected? true)
                     (dissoc :tab/password)
                     (dissoc :tab/user-id))]
    (expect expected actual)))

(defexpect normal-create-tab-without-protection
  (let [query    [{`(tab-mut/create-tab {:tab/name "foo" :tab/password "bar"}) tab-join}]
        result   (protected-parser {:request {:user/id @user-id}} query)
        actual   (get result `tab-mut/create-tab)
        tab-id   (:tab/id actual)
        expected (-> (tab-db/fetch-tab {:tab/id tab-id :user/id @user-id})
                     (assoc :tab/is-protected? false)
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
                                       :error-message "Invalid tab" :error-type :invalid-input
                                       :error-data    {:tab/password ["should be at least 1 characters"]}}}]
    (expect expected actual)))

(defexpect fail-create-tab-with-invalid-protection
  (let [query    [{`(tab-mut/create-tab {:tab/name "foo" :tab/password "bar" :tab/is-protected? "boo"}) tab-join}]
        result   (protected-parser {:request {:user/id @user-id}} query)
        actual   result
        expected {`tab-mut/create-tab {:error         true
                                       :error-message "Invalid tab" :error-type :invalid-input
                                       :error-data    {:tab/is-protected? ["should be a boolean"]}}}]
    (expect expected actual)))

(defexpect normal-patch-tab-with-new-name-and-password
  (let [tab              (tab-db/create-tab {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id})
        tab-id           (:tab/id tab)
        new-tab-name     "fim"
        new-tab-password "baz"
        query            [{`(tab-mut/patch-tab {:tab/id ~tab-id :tab/name ~new-tab-name :tab/password ~new-tab-password}) tab-join}]
        result           (protected-parser {:request {:user/id @user-id}} query)
        patched-tab      (get result `tab-mut/patch-tab)
        fetched-tab      (tab-db/fetch-tab {:tab/id tab-id :user/id @user-id})]
    (expect tab-id (:tab/id patched-tab))
    (expect new-tab-name (:tab/name patched-tab))
    (expect #(hashers/check new-tab-password %) (:tab/password fetched-tab))
    (expect (not (:tab/is-protected? patched-tab)))
    (expect inst? (:tab/created patched-tab))
    (expect #(.after % (:tab/updated tab)) (:tab/updated patched-tab))))

(defexpect normal-patch-tab-with-new-name
  (let [tab          (tab-db/create-tab {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id})
        tab-id       (:tab/id tab)
        new-tab-name "fim"
        query        [{`(tab-mut/patch-tab {:tab/id ~tab-id :tab/name ~new-tab-name}) tab-join}]
        result       (protected-parser {:request {:user/id @user-id}} query)
        patched-tab  (get result `tab-mut/patch-tab)]
    (expect tab-id (:tab/id patched-tab))
    (expect new-tab-name (:tab/name patched-tab))
    (expect (not (:tab/is-protected? patched-tab)))
    (expect inst? (:tab/created patched-tab))
    (expect #(.after % (:tab/updated tab)) (:tab/updated patched-tab))))

(defexpect normal-patch-tab-with-new-password
  (let [tab              (tab-db/create-tab {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id})
        tab-id           (:tab/id tab)
        new-tab-password "fim"
        query            [{`(tab-mut/patch-tab {:tab/id ~tab-id :tab/password ~new-tab-password}) tab-join}]
        result           (protected-parser {:request {:user/id @user-id}} query)
        patched-tab      (get result `tab-mut/patch-tab)
        fetched-tab      (tab-db/fetch-tab {:tab/id tab-id :user/id @user-id})]
    (expect tab-id (:tab/id patched-tab))
    (expect (:tab/name tab) (:tab/name patched-tab))
    (expect #(hashers/check new-tab-password %) (:tab/password fetched-tab))
    (expect (not (:tab/is-protected? patched-tab)))
    (expect inst? (:tab/created patched-tab))
    (expect #(.after % (:tab/updated tab)) (:tab/updated patched-tab))))

(defexpect normal-patch-tab-with-protection
  (let [tab         (tab-db/create-tab {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id})
        tab-id      (:tab/id tab)
        query       [{`(tab-mut/patch-tab {:tab/id ~tab-id :tab/is-protected? true}) tab-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        patched-tab (get result `tab-mut/patch-tab)
        fetched-tab (tab-db/fetch-tab {:tab/id tab-id :user/id @user-id})]
    (expect tab-id (:tab/id patched-tab))
    (expect (:tab/name tab) (:tab/name patched-tab))
    (expect (:tab/password tab) (:tab/password fetched-tab))
    (expect (:tab/is-protected? patched-tab))
    (expect inst? (:tab/created patched-tab))
    (expect #(.after % (:tab/updated tab)) (:tab/updated patched-tab))))

(defexpect normal-patch-tab-without-new-name-and-password
  (let [tab         (tab-db/create-tab {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id})
        tab-id      (:tab/id tab)
        query       [{`(tab-mut/patch-tab {:tab/id ~tab-id}) tab-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        patched-tab (get result `tab-mut/patch-tab)
        fetched-tab (tab-db/fetch-tab {:tab/id tab-id :user/id @user-id})]
    (expect tab-id (:tab/id patched-tab))
    (expect (:tab/name tab) (:tab/name patched-tab))
    (expect (:tab/password tab) (:tab/password fetched-tab))
    (expect (not (:tab/is-protected? patched-tab)))
    (expect inst? (:tab/created patched-tab))
    (expect #(.after % (:tab/updated tab)) (:tab/updated patched-tab))))

(defexpect fail-patch-tab-with-invalid-name
  (let [tab    (tab-db/create-tab {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id})
        tab-id (:tab/id tab)
        query  [{`(tab-mut/patch-tab {:tab/id ~tab-id :tab/name ""}) tab-join}]
        result (protected-parser {:request {:user/id @user-id}} query)
        error  (get result `tab-mut/patch-tab)]
    (expect {:error         true
             :error-type    :invalid-input
             :error-message "Invalid input"
             :error-data    {:tab/name ["should be at least 1 characters"]}}
            error)))

(defexpect fail-patch-tab-with-invalid-password
  (let [tab    (tab-db/create-tab {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id})
        tab-id (:tab/id tab)
        query  [{`(tab-mut/patch-tab {:tab/id ~tab-id :tab/password ""}) tab-join}]
        result (protected-parser {:request {:user/id @user-id}} query)
        error  (get result `tab-mut/patch-tab)]
    (expect {:error         true
             :error-type    :invalid-input
             :error-message "Invalid input"
             :error-data    {:tab/password ["should be at least 1 characters"]}}
            error)))

(defexpect fail-patch-tab-with-invalid-protection
  (let [tab    (tab-db/create-tab {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id})
        tab-id (:tab/id tab)
        query  [{`(tab-mut/patch-tab {:tab/id ~tab-id :tab/is-protected? "boo"}) tab-join}]
        result (protected-parser {:request {:user/id @user-id}} query)
        error  (get result `tab-mut/patch-tab)]
    (expect {:error         true
             :error-type    :invalid-input
             :error-message "Invalid input"
             :error-data    {:tab/is-protected? ["should be a boolean"]}}
            error)))

(defexpect normal-delete-tab
  (let [tab         (tab-db/create-tab {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id})
        tab-id      (:tab/id tab)
        query       [{`(tab-mut/delete-tab {:tab/id ~tab-id}) tab-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        deleted-tab (get result `tab-mut/delete-tab)]
    (expect tab-id (:tab/id deleted-tab))
    (expect (:tab/name tab) (:tab/name deleted-tab))
    (expect nil (:tab/password deleted-tab))
    (expect (:tab/is-protected? tab) (:tab/is-protected? deleted-tab))
    (expect (:tab/created tab) (:tab/created deleted-tab))
    (expect (:tab/updated tab) (:tab/updated deleted-tab))))

(defexpect normal-delete-tab-with-nonexistent-id
  (let [tab-id      (UUID/randomUUID)
        query       [{`(tab-mut/delete-tab {:tab/id ~tab-id}) tab-join}]
        result      (protected-parser {:request {:user/id @user-id}} query)
        deleted-tab (get result `tab-mut/delete-tab)]
    (expect nil deleted-tab)))

(defexpect fail-delete-tab-with-invalid-id
  (let [tab    (tab-db/create-tab {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id})
        tab-id "foo"
        query  [{`(tab-mut/delete-tab {:tab/id ~tab-id}) tab-join}]
        result (protected-parser {:request {:user/id @user-id}} query)
        error  (get result `tab-mut/delete-tab)]
    (expect {:error         true
             :error-type    :invalid-input
             :error-message "Invalid input"
             :error-data    {:tab/id ["should be a uuid"]}}
            error)))

(comment
  (require '[kaocha.repl :as k])
  (require '[malli.core :as m])
  (m/validate [:map [:a :int] [:b {:optional true} [:maybe :int]]] {:a 3 :b 3})
  (k/run 'shinsetsu.mutations.tab-test)
  (k/run #'shinsetsu.mutations.tab-test/fail-delete-tab-with-invalid-id))
