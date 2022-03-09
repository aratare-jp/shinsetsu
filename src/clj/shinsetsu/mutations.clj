(ns shinsetsu.mutations
  (:require
    [shinsetsu.db :as db]
    [com.wsscode.pathom.connect :as pc :refer [defmutation]]
    [taoensso.timbre :as log]
    [buddy.sign.jwt :as jwt]
    [buddy.hashers :as hashers]))

;; TODO: SPEC THESE SUCKERS!

(defn create-token
  [user]
  (let [secret "my-secret"]
    (jwt/sign {:id (.toString (:user/id user))} secret)))

(defmutation login
  [_ {:user/keys [username password] :as user}]
  {::pc/params #{:username :password}
   ::pc/output [:token]}
  (log/info "User with username" username "is attempting to login...")
  ;; FIXME: Move secret into env variable
  (let [user          (db/fetch-user-by-username user)
        invalid-token {:token :invalid}]
    (if (and user (hashers/check password (:user/password user)))
      {:token (create-token user)}
      invalid-token)))

(defmutation register
  [_ {:user/keys [username] :as user}]
  {::pc/params #{:user/username :user/password}
   ::pc/output [:token]}
  (log/info "Someone is attempting to register with username" username)
  (let [user (update user :user/password hashers/derive)]
    (if-let [user (db/create-user user)]
      {:token (create-token user)}
      {:token :invalid})))

(def mutations [login register])

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
