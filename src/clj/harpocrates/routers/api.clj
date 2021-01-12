(ns harpocrates.routers.api
  (:require [harpocrates.middleware.api :refer [wrap-api]]
            [com.fulcrologic.fulcro.server.api-middleware :as server]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.resource :refer [wrap-resource]]
            [harpocrates.middleware.common :refer [not-found-handler]]
            [harpocrates.parser :refer [api-parser]]))

(def api-routes
  ["/api" {:middleware [[not-found-handler]
                        [wrap-api api-parser]
                        [server/wrap-transit-params]
                        [server/wrap-transit-response]
                        [wrap-content-type]]}])