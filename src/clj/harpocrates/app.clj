(ns harpocrates.app
  (:require
    [mount.core :refer [defstate]]
    [reitit.ring :as rr]
    [reitit.ring.coercion :as rrc]
    [reitit.ring.middleware.multipart :as multipart]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.parameters :as parameters]
    [ring.middleware.reload :refer [wrap-reload]]
    [harpocrates.routers.api :refer [api-routes]]
    [harpocrates.routers.login :refer [login-routes]]
    [harpocrates.middleware.exception :as exception]
    [harpocrates.config :refer [env]]))

(defstate app
  :start
  (rr/ring-handler
    (rr/router
      [login-routes
       api-routes]
      {:data {:middleware [exception/exception-middleware]}})
    (rr/routes
      (rr/create-resource-handler {:path "/"})
      (rr/create-default-handler))
    {:middleware [(if (:dev? env) wrap-reload)
                  ;; query-params & form-params
                  parameters/parameters-middleware
                  ;; content-negotiation
                  muuntaja/format-negotiate-middleware
                  ;; encoding response body
                  muuntaja/format-response-middleware
                  ;; decoding request body
                  muuntaja/format-request-middleware
                  ;; coercing response bodies
                  rrc/coerce-response-middleware
                  ;; coercing request parameters
                  rrc/coerce-request-middleware
                  ;; multipart
                  multipart/multipart-middleware]}))

