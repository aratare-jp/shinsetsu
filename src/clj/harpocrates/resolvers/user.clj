(ns harpocrates.resolvers.user
  (:require [com.wsscode.pathom.connect :as pc]
            [harpocrates.db.core :refer [*db*]]
            [taoensso.timbre :as log]
            [buddy.sign.jws :as jws]
            [harpocrates.config :refer [env]]
            [puget.printer :refer [pprint]]))

(pc/defresolver user-resolver
  [env {:user/keys [id]}]
  {::pc/input  #{:user/id}
   ::pc/output [:user/email]}
  {:user/id    id
   :user/email (get-in @*db* [:user/id id :user/email])})

(pc/defresolver current-user-resolver [env _]
  {::pc/output [{:session/current-user [:user/id]}]}
  (let [{:user/keys [id email]} (log/spy :info (-> env :request :session))]
    {:session/current-user
     (if id
       {:user/email  email
        :user/id     id
        :user/valid? true}
       {:user/id     :nobody
        :user/valid? false})}))

(def resolvers [user-resolver
                current-user-resolver])