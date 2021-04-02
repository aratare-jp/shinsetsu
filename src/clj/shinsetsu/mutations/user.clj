(ns shinsetsu.mutations.user
  (:require [com.fulcrologic.fulcro.server.api-middleware :refer [augment-response]]
            [com.wsscode.pathom.connect :as pc]
            [taoensso.timbre :as log]
            [shinsetsu.db.core :refer [db]]
            [shinsetsu.config :refer [env]]
            [shinsetsu.schemas :refer :all]
            [buddy.sign.jws :as jws]
            [buddy.hashers :as hashers]
            [puget.printer :refer [pprint]]
            [schema.core :as s]
            [shinsetsu.db.user :as db])
  (:import [java.util Date]))

(pc/defmutation login
  [env {:user/keys [username password]}]
  {::pc/params #{:user/username :user/password}
   ::pc/output [:user/id :user/valid?]}
  (s/validate NonEmptyContinuousStr username)
  (s/validate NonEmptyContinuousStr password)
  (log/info "Login with" username)
  #_(let [secret  (:secret env)
        user    (db/read-user-by-username {:user/username username})
        user-id (:user-id user)
        token   (jws/sign user-id secret)]
    (if (hashers/check password (:user/password user))
      (do
        (db/create-current-user {:user/id      user-id
                                 :user/token   token
                                 :user/created (new Date)
                                 :user/updated (new Date)})
        (augment-response
          {:user/id user-id :user/valid? true}
          (fn [ring-resp] (assoc ring-resp :session token))))
      {:user/valid? false})))

(pc/defmutation logout
  [env {:user/keys [username password]}]
  {::pc/params #{:user/username :user/password}
   ::pc/output [:user/id :user/valid?]}
  (s/validate NonEmptyContinuousStr username)
  (s/validate NonEmptyContinuousStr password)
  (log/info "Logging out with" username)
  #_(let [secret    (:secret env)
        jws-token (-> env :request :session)
        user-id   (jws/unsign jws-token secret)
        tokens    (db/check-current-user {:user/id user-id})]
    (if (some #(= % jws-token) tokens)
      (do
        (log/info "Retiring token" jws-token)
        (db/delete-current-user {:user/id user-id :user/token jws-token})
        (augment-response
          {:user/id :nobody :user/valid? false}
          (fn [ring-resp] (assoc ring-resp :session {}))))
      (do
        (log/warn username "tried to log out with invalid token" jws-token)
        {:user/id :nobody :user/valid? false}))))

(def mutations [login logout])
