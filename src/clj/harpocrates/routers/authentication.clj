(ns harpocrates.routers.authentication
  (:require [reitit.coercion.schema]
            [schema.core :as s]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [buddy.hashers :as hashers]
            [buddy.sign.jwt :as jwt]
            [harpocrates.db.core :refer [*db*]]
            [puget.printer :as puget]
            [ring.middleware.content-type :refer [wrap-content-type]]))

(defn on-login [{{{:keys [username password]} :form} :parameters :as rest}]
  (puget/pprint rest)
  (if-let [user (get-in @*db* [:user/id username])]
    (if (hashers/check password (:password user))
      {:token "blah"}
      {:status  401
       :message "Not authenticated"})
    {:status  401
     :message "Not authenticated"}))

(defn on-signup [{{{:keys [username password]} :form} :parameters}]
  (if-let [_ (get-in @*db* [:user/id username])]
    {:status  409
     :message "Account already exists"}
    (let [hashed-pwd (hashers/derive password)]
      (swap! *db* assoc-in [:user/id username] {:username username
                                                :password hashed-pwd})
      {:token "bluh"})))

(def login-routes
  ["/login" {:post {:coercion   reitit.coercion.schema/coercion
                    :parameters {:form {:username s/Str
                                        :password s/Str}}
                    :responses  {200 {:body {:token    s/Str
                                             :username s/Str
                                             :password s/Str}}}
                    :handler    on-login}}])

(def signup-routes
  ["/signup" {:post {:coercion   reitit.coercion.schema/coercion
                     :parameters {:form {:username s/Str
                                         :password s/Str}}
                     :responses  {200 {:body {:token    s/Str
                                              :username s/Str
                                              :password s/Str}}}
                     :handler    on-signup}}])
