(ns shinsetsu.application
  (:require
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.networking.http-remote :as http]
    [taoensso.timbre :as log]))

(def login-token (atom nil))

(defn wrap-auth-token
  ([token] (wrap-auth-token identity token))
  ([handler token]
   (fn [req]
     (handler (update req :headers assoc "Authorization" (str "Bearer " @token))))))

(def req-middleware
  (-> (wrap-auth-token login-token)
      (http/wrap-fulcro-request)))

(defn contain-errors?
  [body]
  (let [values (vals body)]
    (reduce (fn [acc {:keys [error]}]
              (if error
                (reduced true)
                acc))
            false
            values)))

(defonce app (app/fulcro-app {:remote-error? (fn [{:keys [body] :as result}]
                                               (or (app/default-remote-error? result)
                                                   (contain-errors? body)))
                              :remotes
                              {:auth   (http/fulcro-http-remote {:url "auth"})
                               :remote (http/fulcro-http-remote {:request-middleware req-middleware})}}))
