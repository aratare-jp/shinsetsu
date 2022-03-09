(ns shinsetsu.application
  (:require
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.networking.http-remote :as http]))

(def login-token (atom nil))

(defn wrap-auth-token
  ([token] (wrap-auth-token identity token))
  ([handler token]
   (fn [req]
     (if token
       (handler (update req :headers assoc "Authorization" (str "Bearer" token)))
       (handler req)))))

(def req-middleware
  (-> (wrap-auth-token @login-token)
      (http/wrap-fulcro-request)))

(defonce app (app/fulcro-app {:remotes
                              {:remote (http/fulcro-http-remote {:request-middleware req-middleware})}}))
