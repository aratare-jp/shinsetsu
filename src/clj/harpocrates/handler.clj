(ns harpocrates.handler
  (:require
    [harpocrates.middleware :as middleware]
    [harpocrates.layout :refer [error-page]]
    [harpocrates.routes.home :refer [home-routes]]
    [harpocrates.env :refer [defaults]]
    [harpocrates.routes.services :refer [service-routes]]
    [reitit.ring :as ring]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.webjars :refer [wrap-webjars]]
    [mount.core :as mount]))

(mount/defstate init-app
  :start ((or (:init defaults) (fn [])))
  :stop ((or (:stop defaults) (fn []))))

(mount/defstate app-routes
  :start
  (ring/ring-handler
    (ring/router
      [(home-routes)
       (service-routes)])
    (ring/routes
      (ring/create-resource-handler
        {:path "/"}))))

(defn app []
  (middleware/wrap-base #'app-routes))
