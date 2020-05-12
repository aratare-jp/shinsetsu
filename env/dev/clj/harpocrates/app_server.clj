(ns harpocrates.app-server
  (:require
    [ring.util.response :refer [resource-response content-type not-found]]))

(def route-set
  #{"/"
    "/login"
    "/logout"})

(defn handler
  [req]
  (or
    (when (route-set (:uri req))
      (some-> (resource-response "index.html" {:root "public"})
              (content-type "text/html; charset=utf-8")))
    (not-found "Not found")))
