(ns shinsetsu.mutations
  (:require
    [shinsetsu.config :as config]
    [shinsetsu.db :as db]
    [com.wsscode.pathom.connect :as pc :refer [defmutation]]
    [taoensso.timbre :as log]
    [buddy.sign.jwt :as jwt]
    [buddy.hashers :as hashers]))

;; TODO: SPEC THESE SUCKERS!

(defn create-token
  [user]
  (let [secret (:secret config/env)]
    (jwt/sign {:user/id (.toString (:user/id user))} secret)))

(defmutation login
  [_ {:user/keys [username password] :as user}]
  {::pc/params #{:username :password}
   ::pc/output [:token]}
  (log/info "User with username" username "is attempting to login...")
  (let [user          (db/fetch-user-by-username user)
        invalid-token {:token :invalid}]
    (if (and user (hashers/check password (:user/password user)))
      {:token (create-token user)}
      (do
        (log/warn "User with username" username "attempts to login with a wrong password")
        invalid-token))))

(defmutation register
  [_ {:user/keys [username] :as user}]
  {::pc/params #{:user/username :user/password}
   ::pc/output [:token]}
  (log/info "Someone is attempting to register with username" username)
  (let [invalid-token {:token :invalid}]
    ;; Can have race condition when multiple user registers.
    (if (db/fetch-user-by-username user)
      (do
        (log/warn "User with username" username "already exists")
        invalid-token)
      (let [user (update user :user/password hashers/derive)]
        (if-let [user (db/create-user user)]
          {:token (create-token user)}
          invalid-token)))))

(defmutation create-tab
  [{{:user/keys [id username] :as user} :user} tab]
  {::pc/params #{:tab/name :tab/password}
   ::pc/output [:tab/name :tab/is-protected? :tab/created :tab/updated]}
  (log/info "User with username" username "is attempting to create a new tab")
  (-> tab
      (assoc :tab/user-id id)
      (db/create-tab)))

(def public-mutations [login register])
(def protected-mutations [create-tab])

(comment
  (user/restart)
  (require '[buddy.sign.jwt :as jwt])
  (jwt/sign {:id "blah"} "secret")
  (def user {:user/username "foo" :user/password "bar"})
  (def password "bar")
  (db/fetch-user-by-username user)
  (let [secret        "my-secret"
        user          (db/fetch-user-by-username user)
        invalid-token {:token :invalid}]
    (print (:user/password user))
    (if (and user (hashers/check password (:user/password user)))
      {:token (jwt/sign {:id (.toString (:user/id user))} secret)}
      invalid-token))
  (let [user (update user :password hashers/derive)]
    (print user)
    (if (db/create-user [user])
      {:token "hello-world!"}
      {:token :invalid}))
  (login nil {:username "fim" :password "fom"})
  (db/fetch-user-by-username {:username "fim"}))
