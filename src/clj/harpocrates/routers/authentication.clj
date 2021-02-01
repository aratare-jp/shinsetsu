(ns harpocrates.routers.authentication
  (:require [reitit.coercion.schema]
            [schema.core :as s]
            [buddy.hashers :as hashers]
            [buddy.sign.jws :as jws]
            [harpocrates.db.core :refer [*db*]]
            [harpocrates.config :refer [env]]
            [ring.util.response :as response]
            [puget.printer :refer [pprint]]))

(defn on-login [{{{:keys [username password]} :form} :parameters}]
  (let [failed-res {:status 401 :body {:message "Not authenticated"}}
        secret     (:secret env)]
    (if-let [user (get-in @*db* [:user/id username])]
      (if (hashers/check password (:password user))
        (response/response {:token (jws/sign username secret)})
        failed-res)
      failed-res)))

(defn on-signup [{{{:keys [username password]} :form} :parameters}]
  (pprint env)
  (let [already-created-res {:status 409 :body {:message "Already created"}}
        secret              (:secret env)]
    (if-let [_ (get-in @*db* [:user/id username])]
      already-created-res
      (let [hashed-pwd (hashers/derive password)]
        (swap! *db* assoc-in [:user/id username] {:username username :password hashed-pwd})
        (response/response {:token (jws/sign username secret)})))))

(def login-routes
  ["/login" {:post {:coercion   reitit.coercion.schema/coercion
                    :parameters {:form {:username s/Str
                                        :password s/Str}}
                    :responses  {200 {:body {:token s/Str}}
                                 401 {:body {:message s/Str}}}
                    :handler    on-login}}])

(def signup-routes
  ["/signup" {:post {:coercion   reitit.coercion.schema/coercion
                     :parameters {:form {:username s/Str
                                         :password s/Str}}
                     :responses  {200 {:body {:token s/Str}}
                                  409 {:body {:message s/Str}}}
                     :handler    on-signup}}])
