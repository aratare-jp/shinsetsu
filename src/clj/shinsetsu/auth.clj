(ns shinsetsu.auth
  (:require
    [shinsetsu.config :as config]
    [buddy.sign.jwt :as jwt]
    [taoensso.timbre :as log]
    [clojure.string :as string])
  (:import [java.util UUID]))

(defn wrap-auth
  [handler]
  (fn [req]
    (if-let [token (-> req (get-in [:headers "authorization"]) (string/split #" ") second)]
      (let [token (-> token
                      (jwt/unsign (:secret config/env))
                      (update :user/id #(UUID/fromString %)))]
        (handler (merge req token)))
      {:status-code 401
       :message     "Invalid authentication"})))

(comment
  (user/restart))
