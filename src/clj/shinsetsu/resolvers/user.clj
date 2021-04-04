(ns shinsetsu.resolvers.user
  (:require [com.wsscode.pathom.connect :as pc]
            [shinsetsu.db.user :refer :all]
            [shinsetsu.db.session :refer :all]
            [shinsetsu.db.tab :refer :all]
            [shinsetsu.db.core :refer [db]]
            [shinsetsu.schemas :refer :all]
            [taoensso.timbre :as log]
            [buddy.sign.jws :as jws]
            [shinsetsu.config :refer [env]]))

(pc/defresolver user-resolver
  [env {:user/keys [id] :as data}]
  {::pc/input  #{:user/id}
   ::pc/output [:user/username :user/image :user/created :user/updated]}
  (log/info "Retrieving data for user" id)
  (let [secret  (:secret env)
        token   (-> env :request :session)
        user-id (jws/unsign token secret)]
    (if (= user-id id)
      (read-user db data)
      (throw (ex-info "Cannot query other user's data" {})))))

(pc/defresolver user-tabs-resolver
  [env {:user/keys [id] :as data}]
  {::pc/input  #{:user/id}
   ::pc/output [{:user/tabs [:tabs/id]}]}
  (log/info "Retrieving data for user" id)
  (let [secret  (:secret env)
        token   (-> env :request :session)
        user-id (jws/unsign token secret)]
    (if (= user-id id)
      (read-user-tab db data)
      (throw (ex-info "Cannot query other user's data" {})))))

(pc/defresolver current-user-resolver
  [env _]
  {::pc/output [{:session/current-user [:user/id :user/valid]}]}
  (let [secret    (:secret env)
        jws-token (-> env :request :session)]
    (if jws-token
      (let [user-id (jws/unsign jws-token secret)
            token   (check-session db {:session/id user-id :session/token jws-token})]
        (if token
          {:session/current-user {:user/id user-id :user/valid true}}
          {:user/id :nobody :user/valid? false}))
      {:user/id :nobody :user/valid? false})))

(def resolvers [user-resolver user-tabs-resolver current-user-resolver])