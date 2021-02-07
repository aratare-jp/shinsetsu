(ns harpocrates.mutations.user
  (:require [com.fulcrologic.fulcro.server.api-middleware :refer [augment-response]]
            [com.wsscode.pathom.connect :as pc]
            [taoensso.timbre :as log]
            [harpocrates.db.core :refer [*db*]]
            [harpocrates.config :refer [env]]
            [buddy.sign.jws :as jws]
            [buddy.hashers :as hashers]
            [puget.printer :refer [pprint]]))

(pc/defmutation login
  [env {:user/keys [username password]}]
  {::pc/params #{:user/username :user/password}
   ::pc/output [:user/id :user/token]}
  (let [user   (get-in @*db* [:user/id username])
        secret (:secret env)]
    (if (hashers/check password (:user/password user))
      {:user/id    (:user/id user)
       :user/token (jws/sign "test@test.com" secret)}
      (throw (ex-info nil {:status-code 401
                           :message     "Incorrect username or password"})))))

(pc/defmutation logout
  [env _]
  {::pc/output [:user/id]}
  ;; TODO: Clear the logged-in cache.
  {:user/id :nobody})

(def mutations [login
                logout])

(comment
  (user/restart)
  (require '[buddy.hashers :as hashers])
  (require '[buddy.sign.jws :as jws])
  (require '[harpocrates.db.core :refer [*db*]])
  (get-in @*db* [:user/id "test@test.com"])
  (require '[harpocrates.config :refer [env]])
  (mount.core/start #'env)
  env
  (:secret env)
  (jws/sign "test@test.com" (:secret env)))