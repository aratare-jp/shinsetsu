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
  [env {:user/keys [email password]}]
  {::pc/params #{:user/email :user/password}
   ::pc/output [:user/id :user/valid?]}
  (log/info "Login" email)
  (let [secret   (:secret env)
        user-map (get @*db* :user/id)
        subject  (second (first (filter (fn [[_ v]] (= (:user/email v) email)) user-map)))]
    (if (hashers/check password (:user/password subject))
      (augment-response
        {:user/email  email
         :user/id     (:user/id subject)
         :user/valid? true}
        (fn [ring-resp] (assoc ring-resp :session (:user/id subject))))
      {:user/valid? false})))

(pc/defmutation logout [env {:user/keys [email password]}]
  {::pc/params #{:user/email :user/password}
   ::pc/output [:user/id :user/valid?]}
  (augment-response
    {:user/id     :nobody
     :user/valid? false}
    (fn [ring-resp] (assoc ring-resp :session {}))))

(def mutations [login
                logout])
