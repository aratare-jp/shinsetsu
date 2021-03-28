(ns shinsetsu.routers.api
  (:require [shinsetsu.middleware.parser :refer [parser-handler]]
            [shinsetsu.middleware.authentication :refer [wrap-auth]]
            [shinsetsu.parser :refer [pathom-parser api-parser]]
            [com.fulcrologic.fulcro.server.api-middleware :as server]
            [puget.printer :refer [pprint]]))

(def api-routes
  ["/api" {:post {:middleware [[server/wrap-transit-params]
                               [server/wrap-transit-response]]
                  :handler    (parser-handler pathom-parser)}}])
