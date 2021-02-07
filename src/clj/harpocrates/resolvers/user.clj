(ns harpocrates.resolvers.user
  (:require [com.wsscode.pathom.connect :as pc]
            [harpocrates.db.core :refer [*db*]]
            [taoensso.timbre :as log]
            [buddy.sign.jws :as jws]
            [harpocrates.config :refer [env]]))

(pc/defresolver user-resolver
  [env {:user/keys [id]}]
  {::pc/input  #{:user/id}
   ::pc/output [:user/username]}
  {:user/id    id
   :user/email (get-in @*db* [:user/id id :user/username])})

;; TODO: Consider whether this is necessary
(pc/defresolver current-user-resolver
  [env _]
  {::pc/output [{:session/current-user [:user/id]}]}
  (let [encrypted-session (-> env :request :session)
        {:user/keys [id email]} (log/spy (jws/unsign encrypted-session (:secret env)))]
    {:session/current-user
     (if id
       {:user/email  email
        :user/id     id
        :user/valid? true}
       {:user/id     id
        :user/valid? false})}))

(def resolvers [user-resolver
                current-user-resolver])