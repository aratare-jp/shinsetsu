(ns harpocrates.app
  (:require
    [mount.core :refer [defstate]]
    [reitit.ring :as rr]
    [reitit.ring.coercion :as rrc]
    [reitit.ring.middleware.multipart :as multipart]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.parameters :as parameters]
    [ring.middleware.reload :refer [wrap-reload]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [ring.middleware.defaults :refer [wrap-defaults]]
    [harpocrates.routers.api :refer [api-routes]]
    [harpocrates.routers.authentication :refer [login-routes signup-routes]]
    [harpocrates.config :refer [env]]
    [harpocrates.middleware.exception :as exception]))

(defstate app
  :start
  (rr/ring-handler
    (rr/router
      [login-routes
       signup-routes
       api-routes]
      {:data {:middleware [rrc/coerce-exceptions-middleware
                           rrc/coerce-request-middleware
                           rrc/coerce-response-middleware
                           exception/exception-middleware]}})
    (rr/routes
      (rr/create-resource-handler {:path "/"})
      (rr/create-default-handler))
    {:middleware [(if (:dev? env) wrap-reload)
                  [wrap-defaults (into {:security {:anti-forgery         true
                                                   :xss-protection       {:enable? true, :mode :block}
                                                   :frame-options        :sameorigin
                                                   :content-type-options :nosniff}}
                                       (if (or (:dev? env) (:test? env))
                                         {:security {:anti-forgery false}}
                                         {:security {:hsts         true
                                                     :ssl-redirect true}}))]
                  parameters/parameters-middleware
                  muuntaja/format-middleware
                  multipart/multipart-middleware]}))
