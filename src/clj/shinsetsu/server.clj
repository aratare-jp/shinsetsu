(ns shinsetsu.server
  (:require
    [org.httpkit.server :as http]
    [mount.core :refer [defstate start start-with-args stop]]
    [clojure.tools.cli :as cli]
    [taoensso.timbre :as log]
    [shinsetsu.nrepl :as nrepl]
    [shinsetsu.config :refer [env]]
    [shinsetsu.app :refer [app]]))

;; log uncaught exceptions in threads
(Thread/setDefaultUncaughtExceptionHandler
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ thread ex]
      (log/error {:what      :uncaught-exception
                  :exception ex
                  :where     (str "Uncaught exception on" (.getName thread))}))))

(def cli-options
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)]])

(defonce server (atom nil))

(defstate ^{:on-reload :noop} http-server
  :start
  (reset! server (http/run-server
                   app
                   (-> env
                       (assoc :handler app)
                       (assoc :port (or (-> env :options :port) 3000)))))
  :stop
  (when @server
    (@server)
    (reset! server nil)))

(defn- stop-app
  "Stops everything and shutdown the web app."
  []
  (doseq [component (:stopped (stop))]
    (log/info component "stopped"))
  (shutdown-agents))

(defn- start-app
  "Initialise everything and start the web app."
  [args]
  (doseq [component (-> args
                        (cli/parse-opts cli-options)
                        start-with-args
                        :started)]
    (log/info component "started"))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))

(defn -main [& args]
  (start #'shinsetsu.config/env)
  (cond
    (nil? (:jdbcUrl env))
    (do
      (log/error "Database configuration not found, :jdbcUrl environment variable must be set before running")
      (System/exit 1))
    :else
    (start-app args)))

;(ns shinsetsu.server
;  (:require
;    [mount.core :refer [defstate]]
;    [shinsetsu.parser :refer [api-parser]]
;    [org.httpkit.server :as http]
;    [com.fulcrologic.fulcro.server.api-middleware :as server]
;    [ring.middleware.content-type :refer [wrap-content-type]]
;    [ring.middleware.resource :refer [wrap-resource]]
;    [taoensso.timbre :as log]))
;
;(def ^:private not-found-handler
;  (fn [req]
;    {:status  404
;     :headers {"Content-Type" "text/plain"}
;     :body    "Not Found"}))
;
;(def middleware
;  (-> not-found-handler
;      (server/wrap-api {:uri "/api" :parser api-parser})
;      (server/wrap-transit-params)
;      (server/wrap-transit-response)
;      (wrap-resource "public")
;      (wrap-content-type)))
;
;(defonce stop-fn (atom nil))
;
;(defn start
;  []
;  (reset! stop-fn (http/run-server middleware {:port 3000})))
;
;(defn stop
;  []
;  (when @stop-fn
;    (@stop-fn)
;    (reset! stop-fn nil)))
;
;
;(defn start-app
;  []
;  )
