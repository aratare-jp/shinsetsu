(ns harpocrates.application
  (:require
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.networking.http-remote :as http]
    [harpocrates.vars :refer [token]]))

(defn attach-token
  [handler]
  (fn [req]
    (if-let [token @token]
      (handler (assoc req :token token))
      (handler req))))

(defonce app (app/fulcro-app {:remotes
                              {:remote (http/fulcro-http-remote {:request-middleware
                                                                 (-> (http/wrap-fulcro-request)
                                                                     attach-token)})}}))