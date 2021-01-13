(ns harpocrates.routers.api
  (:require [harpocrates.middleware.parser :refer [wrap-parser]]
            [com.fulcrologic.fulcro.server.api-middleware :as server]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.resource :refer [wrap-resource]]
            [harpocrates.middleware.common :refer [not-found-handler]]
            [harpocrates.parser :refer [api-parser]]))

(def api-routes
  ["/api" {:middleware [[wrap-parser api-parser]]}])