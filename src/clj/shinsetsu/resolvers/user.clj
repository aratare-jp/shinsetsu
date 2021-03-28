(ns shinsetsu.resolvers.user
  (:require [com.wsscode.pathom.connect :as pc]
            [shinsetsu.db.user :as db]
            [shinsetsu.schemas :refer :all]
            [taoensso.timbre :as log]
            [buddy.sign.jws :as jws]
            [shinsetsu.config :refer [env]]
            [puget.printer :refer [pprint]]))

(pc/defresolver user-resolver
  [env {:user/keys [id] :as data}]
  {::pc/input  #{:user/id}
   ::pc/output [:user/username :user/image :user/created :user/updated]}
  (let [secret  (:secret env)
        token   (-> env :request :session)
        user-id (jws/unsign token secret)]
    (if (= user-id id)
      (db/read-user data)
      (throw (ex-info "Cannot query other user's data" {})))))

(pc/defresolver user-tabs-resolver
  [env {:user/keys [id] :as data}]
  {::pc/input  #{:user/id}
   ::pc/output [{:user/tabs [:tabs/id]}]}
  (let [secret  (:secret env)
        token   (-> env :request :session)
        user-id (jws/unsign token secret)]
    (if (= user-id id)
      (db/read-user-tab data)
      (throw (ex-info "Cannot query other user's data" {})))))

(pc/defresolver current-user-resolver
  [env _]
  {::pc/output [{:session/current-user [:user/id :user/valid]}]}
  (let [secret    (:secret env)
        jws-token (-> env :request :session)]
    (if jws-token
      (let [user-id (jws/unsign jws-token secret)
            tokens  (db/read-current-user {:user/id user-id})]
        (if tokens
          {:session/current-user {:user/id user-id :user/valid true}}
          {:user/id :nobody :user/valid? false}))
      {:user/id :nobody :user/valid? false})))

(def resolvers [user-resolver user-tabs-resolver current-user-resolver])