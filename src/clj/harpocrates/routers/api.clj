(ns harpocrates.routers.api
  (:require [harpocrates.middleware.parser :refer [wrap-parser]]
            [harpocrates.middleware.authentication :refer [wrap-auth]]
            [harpocrates.parser :refer [api-parser]]))

(def api-routes
  ["/api" {:middleware [wrap-auth
                        [wrap-parser api-parser]]}])