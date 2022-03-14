(ns shinsetsu.app
  (:require
    [shinsetsu.middleware.auth :refer [wrap-auth]]
    [shinsetsu.parser :refer [public-parser-handler protected-parser-handler]]
    [com.fulcrologic.fulcro.server.api-middleware :as server]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.resource :refer [wrap-resource]]
    [reitit.ring :as ring]
    [mount.core :refer [defstate]]))

(defstate app
  :start
  (ring/ring-handler
    (ring/router
      [["/auth" {:post {:handler public-parser-handler}}]
       ["/api" {:post {:middleware [[wrap-auth]]
                       :handler    protected-parser-handler}}]])
    (ring/routes
      (ring/create-resource-handler {:path "/"})
      (ring/create-default-handler))
    {:middleware [[wrap-content-type]
                  [wrap-resource "public"]
                  [server/wrap-transit-response]
                  [server/wrap-transit-params]]}))
