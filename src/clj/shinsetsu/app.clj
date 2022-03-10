(ns shinsetsu.app
  (:require
    [shinsetsu.auth :refer [wrap-auth]]
    [shinsetsu.parser :refer [pathom-handler]]
    [com.fulcrologic.fulcro.server.api-middleware :as server]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.resource :refer [wrap-resource]]
    [reitit.ring :as ring]
    [mount.core :refer [defstate]]))

(defstate app
  :start
  (ring/ring-handler
    (ring/router
      ["/auth" {:post {:handler pathom-handler}}]
      ["/api" {:post {:middleware [[wrap-auth]]
                      :handler    pathom-handler}}])
    (ring/routes
      (ring/create-resource-handler {:path "/"})
      (ring/create-default-handler))
    {:middleware [[wrap-content-type]
                  [wrap-resource "public"]
                  [server/wrap-transit-response]
                  [server/wrap-transit-params]]}))

(comment
  (user/restart)
  (shinsetsu.app/app {:request-method :post :uri "/auth"}))
