(ns shinsetsu.mutations.auth-test
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [shinsetsu.mutations.auth :as auth-mut]
    [shinsetsu.parser :refer [public-parser]]
    [shinsetsu.db.user :as user-db]
    [mount.core :as mount]
    [shinsetsu.config :as config]
    [taoensso.timbre :as log]
    [buddy.sign.jwt :as jwt]
    [buddy.hashers :as hashers])
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
        query    [{`(auth-mut/register {:user/username ~username :user/password ~password}) [:user/token]}]
        uuid     (-> (public-parser {} query)
                     (get-in [`auth-mut/register :user/token])
                     (jwt/unsign secret)
                     (:user/id)
                     (UUID/fromString))
        user     (user-db/fetch-user-by-username {:user/username username})]
    (expect user)
    (expect username (:user/username user))
    (expect #(hashers/check password %) (:user/password user))))

(defexpect fail-register-without-username
  (let [password "bar"
        query    [{`(auth-mut/register {:user/password ~password}) [:user/token]}]
        actual   (public-parser {} query)
        expected {`auth-mut/register {:error         true
                                      :error-type    :invalid-input
                                      :error-message "Invalid input"
                                      :error-data    {:user/username ["missing required key"]}}}]
    (expect expected actual)))

(defexpect fail-register-with-invalid-username
  (let [username ""
        password "bar"
        query    [{`(auth-mut/register {:user/username ~username :user/password ~password}) [:user/token]}]
        actual   (public-parser {} query)
        expected {`auth-mut/register {:error         true
                                      :error-type    :invalid-input
                                      :error-message "Invalid input"
                                      :error-data    {:user/username ["should be at least 1 characters"]}}}]
    (expect expected actual)))

(defexpect fail-register-without-password
  (let [username "foo"
        query    [{`(auth-mut/register {:user/username ~username}) [:user/token]}]
        actual   (public-parser {} query)
        expected {`auth-mut/register {:error         true
                                      :error-type    :invalid-input
                                      :error-message "Invalid input"
                                      :error-data    {:user/password ["missing required key"]}}}]
    (expect expected actual)))

(defexpect fail-register-with-invalid-password
  (let [username "foo"
        password ""
        query    [{`(auth-mut/register {:user/username ~username :user/password ~password}) [:user/token]}]
        actual   (public-parser {} query)
        expected {`auth-mut/register {:error         true
                                      :error-type    :invalid-input
                                      :error-message "Invalid input"
                                      :error-data    {:user/password ["should be at least 1 characters"]}}}]
    (expect expected actual)))

(defexpect normal-login
  (let [username "foo"
        password "bar"
        user     (user-db/create-user {:user/username username :user/password (hashers/derive password)})
        query    [{`(auth-mut/login {:user/username ~username :user/password ~password}) [:user/token]}]
        uuid     (-> (public-parser {} query)
                     (get-in [`auth-mut/login :user/token])
                     (jwt/unsign secret)
                     (:user/id)
                     (UUID/fromString))]
    (expect (:user/id user) uuid)))

(defexpect fail-login-without-username
  (let [password "bar"
        query    [{`(auth-mut/login {:user/password ~password}) [:user/token]}]
        actual   (public-parser {} query)
        expected {`auth-mut/login {:error         true
                                   :error-type    :invalid-input
                                   :error-message "Invalid input"
                                   :error-data    {:user/username ["missing required key"]}}}]
    (expect expected actual)))

(defexpect fail-login-with-invalid-username
  (let [username ""
        password "bar"
        query    [{`(auth-mut/login {:user/username ~username :user/password ~password}) [:user/token]}]
        actual   (public-parser {} query)
        expected {`auth-mut/login {:error         true
                                   :error-type    :invalid-input
                                   :error-message "Invalid input"
                                   :error-data    {:user/username ["should be at least 1 characters"]}}}]
    (expect expected actual)))

(defexpect fail-login-without-password
  (let [username "bar"
        query    [{`(auth-mut/login {:user/username ~username}) [:user/token]}]
        actual   (public-parser {} query)
        expected {`auth-mut/login {:error         true
                                   :error-type    :invalid-input
                                   :error-message "Invalid input"
                                   :error-data    {:user/password ["missing required key"]}}}]
    (expect expected actual)))

(defexpect fail-login-with-invalid-password
  (let [username "foo"
        password ""
        query    [{`(auth-mut/login {:user/username ~username :user/password ~password}) [:user/token]}]
        actual   (public-parser {} query)
        expected {`auth-mut/login {:error         true
                                   :error-type    :invalid-input
                                   :error-message "Invalid input"
                                   :error-data    {:user/password ["should be at least 1 characters"]}}}]
    (expect expected actual)))

(comment
  (require '[kaocha.repl :as k])
  (k/run 'shinsetsu.mutations.auth-test)
  (k/run #'shinsetsu.mutations.auth-test/normal-login))
