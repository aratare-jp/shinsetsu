(ns shinsetsu.middleware.auth-test
  (:require
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.test-utility :refer [db-setup db-cleanup]]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.middleware.auth :as am]
    [mount.core :as mount]
    [buddy.sign.jwt :as jwt]
    [shinsetsu.config :as config]
    [spy.core :as spy]
    [taoensso.timbre :as log])
  (:import [java.util UUID]))

(def user-id (atom nil))

(defn user-setup
  [f]
  (mount/start-with {#'shinsetsu.config/env {:secret "hello-world!"}})
  (let [user (user-db/create-user {:user/username "foo" :user/password "bar"})]
    (reset! user-id (:user/id user))
    (f))
  (mount/stop #'shinsetsu.config/env))

(use-fixtures :once db-setup)
(use-fixtures :each db-cleanup user-setup)

(defexpect normal-auth
  (let [token     (jwt/sign {:user/id @user-id} (:secret config/env))
        req       {:headers {"authorization" (str "Bearer " token)}}
        spy-fn    (spy/spy (fn [r]
                             (let [inner-user-id (:user/id r)]
                               (expect @user-id inner-user-id))))
        actual-fn (am/wrap-auth spy-fn)]
    (actual-fn req)
    (expect 1 (spy/call-count spy-fn))))

(defexpect fail-auth-with-nonexistent-user
  (let [user-id   (UUID/randomUUID)
        token     (jwt/sign {:user/id user-id} (:secret config/env))
        req       {:headers {"authorization" (str "Bearer " token)}}
        spy-fn    (spy/spy (fn [r] (expect false)))
        actual-fn (am/wrap-auth spy-fn)]
    (expect {:body {:error {:status-code 401 :reason :not-exist}}} (actual-fn req))))

(defexpect fail-auth-with-invalid-token
  (let [invalid-token "Totally not real"
        req           {:headers {"authorization" (str "Bearer " invalid-token)}}
        spy-fn        (spy/spy (fn [r] (expect false)))
        actual-fn     (am/wrap-auth spy-fn)]
    (expect {:body {:error {:status-code 401 :reason :invalid-token}}} (actual-fn req))))

(defexpect fail-auth-with-missing-header
  (let [req       {:headers {}}
        spy-fn    (spy/spy (fn [r] (expect false)))
        actual-fn (am/wrap-auth spy-fn)]
    (expect {:body {:error {:status-code 401 :reason :missing-header}}} (actual-fn req))))

(comment
  (require '[kaocha.repl :as k])
  (k/run 'shinsetsu.middleware.auth-test)
  (k/run #'shinsetsu.middleware.auth-test/normal-auth)
  (k/run #'shinsetsu.middleware.auth-test/fail-auth-with-missing-header))
