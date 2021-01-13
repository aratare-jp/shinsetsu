(ns harpocrates.routers.api
  (:require [harpocrates.middleware.parser :refer [wrap-parser]]
            [harpocrates.parser :refer [api-parser]]))

(def api-routes
  ["/api" {:middleware [[wrap-parser api-parser]]}])