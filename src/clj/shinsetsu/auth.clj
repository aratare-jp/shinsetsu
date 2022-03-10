(ns shinsetsu.auth
  (:require
    [shinsetsu.config :as config]
    [buddy.sign.jwt :as jwt]
    [taoensso.timbre :as log]))

(defn wrap-auth
  [handler]
  (fn [req]
    (if-let [token (get-in req [:headers "Authorization"])]
      (do
        (log/info (jwt/unsign token (:secret config/env)))
        (handler req))
      {:status-code 401
       :message     "Invalid authentication"})))
