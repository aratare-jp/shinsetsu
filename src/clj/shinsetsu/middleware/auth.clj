(ns shinsetsu.middleware.auth
  (:require
    [shinsetsu.config :as config]
    [buddy.sign.jwt :as jwt]
    [taoensso.timbre :as log]
    [clojure.string :as string])
  (:import [java.util UUID]))

(defn wrap-auth
  "Parse the request to check for Authorization header. If exists, it will be included in the request map."
  [handler]
  (fn [req]
    (if-let [auth-raw-token (get-in req [:headers "authorization"])]
      (try
        (let [token    (-> auth-raw-token (string/split #"^Bearer ") second)
              unsigned (-> token
                           (jwt/unsign (:secret config/env))
                           (update :user/id #(UUID/fromString %)))]
          (handler (merge req unsigned)))
        (catch Exception e
          (log/error e)
          (ex-info "Invalid or missing token" {:status-code 401})))
      (ex-info "Invalid or missing token" {:status-code 401}))))

(comment
  (user/restart))
