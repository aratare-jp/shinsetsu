(ns shinsetsu.app
  (:require
    [com.fulcrologic.fulcro.networking.file-upload :refer [wrap-mutation-file-uploads]]
    [com.fulcrologic.fulcro.server.api-middleware :refer [wrap-transit-params wrap-transit-response]]
    [mount.core :as m]
    [reitit.ring :as ring]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.multipart-params :refer [wrap-multipart-params]]
    [ring.middleware.resource :refer [wrap-resource]]
    [shinsetsu.middleware.auth :refer [wrap-auth]]
    [shinsetsu.middleware.cors :refer [wrap-cors]]
    [shinsetsu.parser :refer [protected-parser-handler public-parser-handler]]))

(m/defstate app
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
                  [wrap-multipart-params]
                  [wrap-transit-response]
                  [wrap-transit-params]
                  [wrap-mutation-file-uploads {}]
                  [wrap-cors]]}))

(comment
  (user/start)
  (user/restart))
