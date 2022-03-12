(ns shinsetsu.application
  (:require
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.networking.http-remote :as http]))

(def login-token (atom nil))

(defn wrap-auth-token
  ([token] (wrap-auth-token identity token))
  ([handler token]
   (fn [req]
     (handler (update req :headers assoc "Authorization" (str "Bearer " @token))))))

(def req-middleware
  (-> (wrap-auth-token login-token)
      (http/wrap-fulcro-request)))

(defonce app (app/fulcro-app {:remotes
                              {:auth   (http/fulcro-http-remote {:url "auth"})
                               :remote (http/fulcro-http-remote {:request-middleware req-middleware})}}))
