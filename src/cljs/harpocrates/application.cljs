(ns harpocrates.application
  (:require
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.networking.http-remote :as http]))

(defn attach-token
  [handler]
  (fn [req]
    (handler (assoc req :token token))))

(defonce app (app/fulcro-app {:remotes {:remote (http/fulcro-http-remote {:request-middleware (-> (http/wrap-fulcro-request)
                                                                                                  attach-token)})}}))