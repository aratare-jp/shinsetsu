(ns shinsetsu.middleware.auth
  (:require
    [shinsetsu.config :as config]
    [buddy.sign.jwt :as jwt]
    [taoensso.timbre :as log]
    [clojure.string :as string]
    [shinsetsu.db.user :as user-db])
  (:import [java.util UUID]))

(defn wrap-auth
  "Parse the request to check for Authorization header. If exists, it will be included in the request map."
  [handler]
  (fn [req]
    (if-let [auth-raw-token (get-in req [:headers "authorization"])]
      (try
        (let [token    (-> auth-raw-token (string/split #"^Bearer ") (second))
              unsigned (-> token (jwt/unsign (:secret config/env)) (update :user/id #(UUID/fromString %)))
              user-id  (:user/id unsigned)]
          (if (user-db/fetch-user-by-id {:user/id user-id})
            (handler (merge req unsigned))
            {:body {:error {:status-code 401 :reason :not-exist}}}))
        (catch Exception e
          (log/error e)
          {:body {:error {:status-code 401 :reason :invalid-token}}}))
      {:body {:error {:status-code 401 :reason :missing-header}}})))

(comment
  (jwt/unsign "test" "foo")
  (jwt/unsign nil "foo")
  (user/restart))
