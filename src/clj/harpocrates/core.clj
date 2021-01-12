(ns harpocrates.core
  (:require
    [org.httpkit.server :as http]
    [reitit.ring :as rr]
    [harpocrates.routers.login :refer [login-routes]]
    [harpocrates.routers.api :refer [api-routes]]
    [reitit.ring.coercion :as rrc]
    [reitit.ring.middleware.parameters :refer [parameters-middleware]]))

(def app
  (rr/ring-handler
    (rr/router
      [login-routes
       api-routes]
      {:data {:middleware [rrc/coerce-exceptions-middleware
                           rrc/coerce-request-middleware
                           rrc/coerce-response-middleware]}})
    (rr/routes
      (rr/create-resource-handler {:path "/"})
      (rr/create-default-handler))
    {:middleware [parameters-middleware]}))

(defonce stop-fn (atom nil))

(defn start []
  (reset! stop-fn (http/run-server app {:port 3000})))

(defn stop []
  (when @stop-fn
    (@stop-fn)
    (reset! stop-fn nil)))