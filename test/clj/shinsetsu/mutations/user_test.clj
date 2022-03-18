(ns shinsetsu.mutations.user-test
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [shinsetsu.mutations.user :as user-mut]
    [shinsetsu.parser :refer [public-parser protected-parser]]
    [shinsetsu.db.user :as user-db]
    [mount.core :as mount]
    [taoensso.timbre :as log]
    [buddy.sign.jwt :as jwt]
    [buddy.hashers :as hashers]
    [malli.core :as m])
  (:import [java.util UUID]))

(def secret "hello-world!")

(defn config-setup
  [f]
  (mount/start-with {#'shinsetsu.config/env {:secret secret}})
  (f)
  (mount/stop #'shinsetsu.config/env))

(use-fixtures :once db-setup config-setup)
(use-fixtures :each db-cleanup)

(defexpect normal-register
  (let [username "foo"
        password "bar"
        query    [{`(user-mut/register {:user/username ~username :user/password ~password}) [:user/token]}]
        uuid     (-> (public-parser {} query)
                     (get-in [`user-mut/register :user/token])
                     (jwt/unsign secret)
                     (:user/id)
                     (UUID/fromString))
        user     (user-db/fetch-user-by-username {:user/username username})]
    (expect user)
    (expect username (:user/username user))
    (expect #(hashers/check password %) (:user/password user))))

(defexpect fail-register-without-username
  (let [password "bar"
        query    [{`(user-mut/register {:user/password ~password}) [:user/token]}]
        actual   (public-parser {} query)
        expected {`user-mut/register {:error         true
                                      :error-type    :invalid-input
                                      :error-message "Invalid input"
                                      :error-data    {:user/username ["missing required key"]}}}]
    (expect expected actual)))

(defexpect fail-register-with-invalid-username
  (let [username ""
        password "bar"
        query    [{`(user-mut/register {:user/username ~username :user/password ~password}) [:user/token]}]
        actual   (public-parser {} query)
        expected {`user-mut/register {:error         true
                                      :error-type    :invalid-input
                                      :error-message "Invalid input"
                                      :error-data    {:user/username ["should be at least 1 characters"]}}}]
    (expect expected actual)))

(defexpect fail-register-without-password
  (let [username "foo"
        query    [{`(user-mut/register {:user/username ~username}) [:user/token]}]
        actual   (public-parser {} query)
        expected {`user-mut/register {:error         true
                                      :error-type    :invalid-input
                                      :error-message "Invalid input"
                                      :error-data    {:user/password ["missing required key"]}}}]
    (expect expected actual)))

(defexpect fail-register-with-invalid-password
  (let [username "foo"
        password ""
        query    [{`(user-mut/register {:user/username ~username :user/password ~password}) [:user/token]}]
        actual   (public-parser {} query)
        expected {`user-mut/register {:error         true
                                      :error-type    :invalid-input
                                      :error-message "Invalid input"
                                      :error-data    {:user/password ["should be at least 1 characters"]}}}]
    (expect expected actual)))

(defexpect normal-login
  (let [username "foo"
        password "bar"
        user     (user-db/create-user {:user/username username :user/password (hashers/derive password)})
        query    [{`(user-mut/login {:user/username ~username :user/password ~password}) [:user/token]}]
        uuid     (-> (public-parser {} query)
                     (get-in [`user-mut/login :user/token])
                     (jwt/unsign secret)
                     (:user/id)
                     (UUID/fromString))]
    (expect (:user/id user) uuid)))

(defexpect fail-login-without-username
  (let [password "bar"
        query    [{`(user-mut/login {:user/password ~password}) [:user/token]}]
        actual   (public-parser {} query)
        expected {`user-mut/login {:error         true
                                   :error-type    :invalid-input
                                   :error-message "Invalid input"
                                   :error-data    {:user/username ["missing required key"]}}}]
    (expect expected actual)))

(defexpect fail-login-with-invalid-username
  (let [username ""
        password "bar"
        query    [{`(user-mut/login {:user/username ~username :user/password ~password}) [:user/token]}]
        actual   (public-parser {} query)
        expected {`user-mut/login {:error         true
                                   :error-type    :invalid-input
                                   :error-message "Invalid input"
                                   :error-data    {:user/username ["should be at least 1 characters"]}}}]
    (expect expected actual)))

(defexpect fail-login-without-password
  (let [username "bar"
        query    [{`(user-mut/login {:user/username ~username}) [:user/token]}]
        actual   (public-parser {} query)
        expected {`user-mut/login {:error         true
                                   :error-type    :invalid-input
                                   :error-message "Invalid input"
                                   :error-data    {:user/password ["missing required key"]}}}]
    (expect expected actual)))

(defexpect fail-login-with-invalid-password
  (let [username "foo"
        password ""
        query    [{`(user-mut/login {:user/username ~username :user/password ~password}) [:user/token]}]
        actual   (public-parser {} query)
        expected {`user-mut/login {:error         true
                                   :error-type    :invalid-input
                                   :error-message "Invalid input"
                                   :error-data    {:user/password ["should be at least 1 characters"]}}}]
    (expect expected actual)))

(defexpect normal-patch-user-with-new-username-and-password
  (let [username     "foo"
        password     "bar"
        new-username "fim"
        new-password "baz"
        user         (user-db/create-user {:user/username username :user/password (hashers/derive password)})
        user-id      (:user/id user)
        query        [{`(user-mut/patch-user {:user/username ~new-username :user/password ~new-password}) [:user/id]}]
        uuid         (-> (protected-parser {:request {:user/id user-id}} query)
                         (get-in [`user-mut/patch-user :user/id]))
        new-user     (user-db/fetch-user-by-id {:user/id user-id})]
    (expect user-id uuid)
    (expect new-username (:user/username new-user))
    (expect #(hashers/check new-password %) (:user/password new-user))))

(defexpect normal-patch-user-with-new-username
  (let [username     "foo"
        password     "bar"
        new-username "fim"
        user         (user-db/create-user {:user/username username :user/password (hashers/derive password)})
        user-id      (:user/id user)
        query        [{`(user-mut/patch-user {:user/username ~new-username}) [:user/id]}]
        uuid         (-> (protected-parser {:request {:user/id user-id}} query)
                         (get-in [`user-mut/patch-user :user/id]))
        new-user     (user-db/fetch-user-by-id {:user/id user-id})]
    (expect user-id uuid)
    (expect new-username (:user/username new-user))
    (expect #(hashers/check password %) (:user/password new-user))))

(defexpect normal-patch-user-with-new-password
  (let [username     "foo"
        password     "bar"
        new-password "baz"
        user         (user-db/create-user {:user/username username :user/password (hashers/derive password)})
        user-id      (:user/id user)
        query        [{`(user-mut/patch-user {:user/password ~new-password}) [:user/id]}]
        uuid         (-> (protected-parser {:request {:user/id user-id}} query)
                         (get-in [`user-mut/patch-user :user/id]))
        new-user     (user-db/fetch-user-by-id {:user/id user-id})]
    (expect user-id uuid)
    (expect username (:user/username new-user))
    (expect #(hashers/check new-password %) (:user/password new-user))))

(defexpect normal-patch-user-without-username-and-password
  (let [username "foo"
        password "bar"
        user     (user-db/create-user {:user/username username :user/password (hashers/derive password)})
        user-id  (:user/id user)
        query    [{`(user-mut/patch-user {}) [:user/id]}]
        uuid     (-> (protected-parser {:request {:user/id user-id}} query)
                     (get-in [`user-mut/patch-user :user/id]))
        new-user (user-db/fetch-user-by-id {:user/id user-id})]
    (expect user new-user)))

(defexpect fail-patch-user-with-invalid-username
  (let [username     "foo"
        password     "bar"
        new-username ""
        user         (user-db/create-user {:user/username username :user/password (hashers/derive password)})
        user-id      (:user/id user)
        query        [{`(user-mut/patch-user {:user/username ~new-username}) [:user/id]}]
        error        (-> (protected-parser {:request {:user/id user-id}} query)
                         (get `user-mut/patch-user))
        new-user     (user-db/fetch-user-by-id {:user/id user-id})]
    (expect user new-user)
    (expect {:error         true
             :error-type    :invalid-input
             :error-message "Invalid input"
             :error-data    {:user/username ["should be at least 1 characters"]}}
            error)))

(defexpect fail-patch-user-with-invalid-password
  (let [username     "foo"
        password     "bar"
        new-password ""
        user         (user-db/create-user {:user/username username :user/password (hashers/derive password)})
        user-id      (:user/id user)
        query        [{`(user-mut/patch-user {:user/password ~new-password}) [:user/id]}]
        error        (-> (protected-parser {:request {:user/id user-id}} query)
                         (get `user-mut/patch-user))
        new-user     (user-db/fetch-user-by-id {:user/id user-id})]
    (expect user new-user)
    (expect {:error         true
             :error-type    :invalid-input
             :error-message "Invalid input"
             :error-data    {:user/password ["should be at least 1 characters"]}}
            error)))

(comment
  (require '[kaocha.repl :as k])
  (k/run 'shinsetsu.mutations.user-test)
  (require '[malli.core :as m])
  (require '[malli.error :as me])
  (m/validate [:and [:map [:a :string]] [:map [:b :string]]] {:a "boo" :b "bar"})

  (k/run #'shinsetsu.mutations.user-test/normal-patch-user-without-username-and-password))
