(ns shinsetsu.app
  (:require
    [com.fulcrologic.fulcro.networking.file-upload :as fu]
    [com.fulcrologic.fulcro.server.api-middleware :as server]
    [mount.core :as m]
    [reitit.ring :as ring]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.multipart-params :refer [wrap-multipart-params]]
    [ring.middleware.resource :refer [wrap-resource]]
    [shinsetsu.middleware.auth :refer [wrap-auth]]
    [shinsetsu.parser :refer [protected-parser-handler public-parser-handler]]))

(defn wrap-cors
  [handler]
  (fn [req]
    (let [res (handler req)]
      (-> res
          (assoc-in [:headers "Access-Control-Allow-Origin"] "*")
          (assoc-in [:headers "Access-Control-Allow-Headers"] "*")))))

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
                  [server/wrap-transit-response]
                  [server/wrap-transit-params]
                  [fu/wrap-mutation-file-uploads {}]
                  [wrap-cors]]}))

(comment
  (user/start)
  (user/restart))
