(ns shinsetsu.mutations.user
  (:require [com.fulcrologic.fulcro.server.api-middleware :refer [augment-response]]
            [com.wsscode.pathom.connect :as pc]
            [taoensso.timbre :as log]
            [shinsetsu.db.core :refer [db]]
            [shinsetsu.db.user :refer :all]
            [shinsetsu.db.session :refer :all]
            [shinsetsu.config :as config]
            [shinsetsu.schemas :refer :all]
            [buddy.sign.jws :as jws]
            [buddy.hashers :as hashers]
            [puget.printer :refer [pprint]]
            [schema.core :as s])
  (:import [java.time OffsetDateTime ZoneOffset]))

(pc/defmutation login
  [_ {:user/keys [username password]}]
  {::pc/params #{:user/username :user/password}
   ::pc/output [:user/id :user/valid?]}
  (s/validate NonEmptyContinuousStr username)
  (s/validate NonEmptyContinuousStr password)
  (log/info "Login with" username)
  (let [secret (:secret config/env)
        user   (read-user-by-username db {:user/username username})]
    (if user
      (let [user-id (:user/id user)
            token   (jws/sign (.toString user-id) secret)]
        (if (hashers/check password (:user/password user))
          (do
            (create-session db {:session/user-id user-id
                                :session/token   token
                                :session/expired (-> (OffsetDateTime/now)
                                                     (.withOffsetSameInstant ZoneOffset/UTC)
                                                     (.plusMinutes 30))})
            (augment-response
              {:user/id user-id :user/valid? true}
              (fn [ring-resp] (assoc ring-resp :session token))))
          {:user/id :nobody :user/valid? false}))
      {:user/id :nobody :user/valid? false})))

(pc/defmutation logout
  [env {:user/keys [username password]}]
  {::pc/params #{:user/username :user/password}
   ::pc/output [:user/id :user/valid?]}
  (s/validate NonEmptyContinuousStr username)
  (s/validate NonEmptyContinuousStr password)
  (log/info "Logging out with" username)
  (let [secret    (:secret config/env)
        jws-token (-> env :request :session)
        user-id   (jws/unsign jws-token secret)
        tokens    (read-session db {:session/user-id user-id})]
    (if (some #(= % jws-token) tokens)
      (do
        (log/info "Retiring token" jws-token)
        (delete-session db {:session/user-id user-id :session/token jws-token})
        (augment-response
          {:user/id :nobody :user/valid? false}
          (fn [ring-resp] (assoc ring-resp :session {}))))
      (do
        (log/warn username "tried to log out with invalid token" jws-token)
        {:user/id :nobody :user/valid? false}))))

(def mutations [login logout])
