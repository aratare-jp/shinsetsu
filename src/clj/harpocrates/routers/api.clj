(ns harpocrates.routers.api
  (:require [harpocrates.middleware.parser :refer [parser-handler]]
            [harpocrates.middleware.authentication :refer [wrap-auth]]
            [harpocrates.parser :refer [api-parser]]
            [com.fulcrologic.fulcro.server.api-middleware :as server]
            [puget.printer :refer [pprint]]))

(def api-routes
  ["/api" {:post {:middleware [[server/wrap-transit-params]
                               [server/wrap-transit-response]]
                  :handler    (parser-handler api-parser)}}])
