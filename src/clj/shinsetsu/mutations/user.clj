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
  (:import [java.time OffsetDateTime ZoneOffset]
           [java.util UUID]))

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
  [env _]
  {::pc/output [:user/id :user/valid?]}
  (let [secret    (:secret config/env)
        jws-token (-> env :request :session)
        user-id   (UUID/fromString (new String (jws/unsign jws-token secret)))
        token     (check-session db {:session/user-id user-id
                                     :session/token   jws-token})]
    (if token
      (do
        (log/info "Retiring token" jws-token "of user" user-id)
        (delete-session db {:session/user-id user-id :session/token jws-token}))
      (log/warn user-id "tried to log out with invalid token" jws-token))
    (augment-response
      {:user/id :nobody :user/valid? false}
      (fn [ring-resp] (assoc ring-resp :session {})))))

(def mutations [login logout])
