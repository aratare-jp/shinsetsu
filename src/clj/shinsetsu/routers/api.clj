(ns shinsetsu.routers.api
  (:require [shinsetsu.middleware.parser :refer [parser-handler]]
            [shinsetsu.middleware.authentication :refer [wrap-auth]]
            [shinsetsu.parser :refer [pathom-parser]]
            [mount.core :refer [defstate]]))

(defstate api-routes
  :start
  ["/api" {:post {:handler (parser-handler pathom-parser)}}])
