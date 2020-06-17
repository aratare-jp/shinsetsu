(ns harpocrates.middleware
  (:require
    [harpocrates.env :refer [defaults]]
    [harpocrates.layout :refer [error-page]]
    [harpocrates.config :refer [env]]
    [harpocrates.middleware.formats :as formats]
    [harpocrates.parser :refer [api-parser]]
    [clojure.tools.logging :as log]
    [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
    [muuntaja.middleware :refer [wrap-format wrap-params]]
    [ring.middleware.cors :refer [wrap-cors]]
    [ring.middleware.flash :refer [wrap-flash]]
    [ring.middleware.session.cookie :refer [cookie-store]]
    [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
    [buddy.auth :refer [authenticated? throw-unauthorized]]
    [buddy.auth.backends :refer [jws]]
    [buddy.auth.middleware :refer [wrap-authentication]]
    [clojure.tools.logging :as log]
    [ring.middleware.reload :refer [wrap-reload]]
    [mount.core :refer [defstate]]
    [com.fulcrologic.fulcro.server.api-middleware :as server]))

(def secret "secret")
(def backend (jws {:secret secret}))

(defn wrap-internal-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (log/error t (.getMessage t))
        (error-page {:status  500
                     :title   "Something very bad has happened!"
                     :message "We've dispatched a team of highly trained gnomes to take care of the problem."})))))

(defn wrap-csrf [handler]
  (wrap-anti-forgery
    handler
    {:error-response
     (error-page
       {:status 403
        :title  "Invalid anti-forgery token"})}))

(defn wrap-formats [handler]
  (let [wrapped (-> handler wrap-params (wrap-format formats/instance))]
    (fn [request]
      ;; disable wrap-formats for websockets
      ;; since they're not compatible with this middleware
      ((if (:websocket? request) handler wrapped) request))))

(defn wrap-base-auth [handler]
  (fn [req]
    (if (authenticated? req)
      (do
        (log/debug "Authenticated!")
        (handler req))
      (do
        (log/debug "Failed authentication")
        {:status  402
         :message "Unauthenticated"}))))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      (server/wrap-api {:uri "/api" :parser api-parser})
      (server/wrap-transit-params)
      (server/wrap-transit-response)
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:security :anti-forgery] false)
            (assoc-in [:session :store] (cookie-store))
            (assoc-in [:session :cookie-name] "example-app-sessions")))
      wrap-internal-error))
