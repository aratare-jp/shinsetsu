(ns shinsetsu.auth
  (:require
    [buddy.sign.jwt :as jwt]
    [taoensso.timbre :as log]))

(defn wrap-auth
  [handler]
  (fn [req]
    (if-let [token (get-in req [:headers "Authorization"])]
      (do
        (log/info (jwt/unsign token "my-secret"))
        ;; TODO: Proper token validation
        (handler req))
      {:status-code 401
       :message     "Invalid authentication"})))
