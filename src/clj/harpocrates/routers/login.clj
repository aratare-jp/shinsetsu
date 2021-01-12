(ns harpocrates.routers.login
  (:require [reitit.coercion.schema]
            [schema.core :as s]
            [ring.middleware.content-type :refer [wrap-content-type]]))

(defn on-login [{{{:keys [username password]} :form} :parameters}]
  {:status 200
   :body   {:token    "hello world!"
            :username username
            :password password}})

(def login-routes
  ["/login" {:post       {:coercion   reitit.coercion.schema/coercion
                          :parameters {:form {:username s/Str
                                              :password s/Str}}
                          :responses  {200 {:body {:token    s/Str
                                                   :username s/Str
                                                   :password s/Str}}}
                          :handler    on-login}
             :middleware [wrap-content-type]}])
