(ns shinsetsu.mutations.user
  (:require
    [buddy.hashers :as hashers]
    [buddy.sign.jwt :as jwt]
    [com.wsscode.pathom.connect :as pc :refer [defmutation]]
    [malli.core :as m]
    [malli.error :as me]
    [shinsetsu.config :as config]
    [shinsetsu.db.user :as db]
    [shinsetsu.schema :as s]
    [taoensso.timbre :as log]))

(defn create-token
  [user]
  (let [secret (:secret config/env)]
    (jwt/sign {:user/id (.toString (:user/id user))} secret)))

(defn hash-password
  [user]
  (if-let [password (:user/password user)]
    (merge user {:user/password (hashers/derive password)})
    user))

(defmutation login
  [_ {:user/keys [username password] :as input}]
  {::pc/params #{:user/username :user/password}
   ::pc/output [:user/token]}
  (if-let [err (m/explain s/user-spec input)]
    (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "User with username" username "is attempting to login")
      (if-let [user (db/fetch-user-by-username {:user/username username})]
        (if (and user (hashers/check password (:user/password user)))
          (do
            (log/info "Credentials correct. Logging in user with ID" (:user/id user))
            {:user/token (create-token user)})
          (do
            (log/warn "User with username" username "attempts to login with a wrong password")
            (throw (ex-info "Wrong username or password" {:error-type :wrong-credentials}))))
        (throw (ex-info "Wrong username or password" {:error-type :wrong-credentials}))))))

(defmutation register
  [_ {:user/keys [username] :as input}]
  {::pc/params #{:user/username :user/password}
   ::pc/output [:user/token]}
  (if-let [err (m/explain s/user-spec input)]
    (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
    (do
      (log/info "Someone is attempting to register with username" username)
      ;; Can have race condition when multiple user registers.
      ;; Move to transaction.
      (if (db/fetch-user-by-username {:user/username username})
        (do
          (log/warn "User with username" username "already exists")
          (throw (ex-info "User already exists" {:error-type :duplicate-user})))
        (let [user (-> input hash-password db/create-user)]
          (log/info "User registered successfully with new ID" (:user/id user))
          {:user/token (create-token user)})))))

(defmutation patch-user
  [{{user-id :user/id} :request} patch-data]
  {::pc/params #{:user/username :user/password}
   ::pc/output [:user/id]}
  (if (empty? patch-data)
    {:user/id user-id}
    (let [patch-data (merge patch-data {:user/id user-id})]
      (if-let [err (m/explain s/user-update-spec patch-data)]
        (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
        (do
          (log/info "User with ID" user-id "is attempting to update their info")
          (let [patched-user (-> patch-data hash-password db/patch-user (dissoc :user/password))]
            (log/info "User with ID" user-id "patched successfully")
            patched-user))))))
