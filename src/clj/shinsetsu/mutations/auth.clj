(ns shinsetsu.mutations.auth
  (:require
    [shinsetsu.config :as config]
    [shinsetsu.db.user :as db]
    [com.wsscode.pathom.connect :as pc :refer [defmutation]]
    [taoensso.timbre :as log]
    [buddy.sign.jwt :as jwt]
    [buddy.hashers :as hashers]
    [malli.core :as m]
    [shinsetsu.schema :as s]
    [malli.error :as me]))

;; TODO: SPEC THESE SUCKERS!

(defn create-token
  [user]
  (let [secret (:secret config/env)]
    (jwt/sign {:user/id (.toString (:user/id user))} secret)))

(defmutation login
  [_ {:user/keys [username password] :as input}]
  {::pc/params #{:user/username :user/password}
   ::pc/output [:user/token]}
  (if-let [err (m/explain [:map {:closed true} [:user/username s/non-empty-string] [:user/password s/non-empty-string]] input)]
    (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "User with username" username "is attempting to login")
      (let [user (db/fetch-user-by-username {:user/username username})]
        (if (and user (hashers/check password (:user/password user)))
          {:user/token (create-token user)}
          (do
            (log/warn "User with username" username "attempts to login with a wrong password")
            (throw (ex-info "Wrong password" {:error-type :wrong-password}))))))))

(defmutation register
  [_ {:user/keys [username password] :as input}]
  {::pc/params #{:user/username :user/password}
   ::pc/output [:user/token]}
  (if-let [err (m/explain [:map {:closed true} [:user/username s/non-empty-string] [:user/password s/non-empty-string]] input)]
    (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Someone is attempting to register with username" username)
      ;; Can have race condition when multiple user registers.
      (if (db/fetch-user-by-username {:user/username username})
        (do
          (log/warn "User with username" username "already exists")
          (throw (ex-info "User already exists" {:error-type :duplicate-user})))
        (let [user (-> (update input :user/password hashers/derive) (db/create-user))]
          {:user/token (create-token user)})))))
