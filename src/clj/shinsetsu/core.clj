(ns shinsetsu.core
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

(defstate ^{:on-reload :noop} repl-server
  :start
  (when (:nrepl-port env)
    (nrepl/start {:bind (:nrepl-bind env)
                  :port (:nrepl-port env)}))
  :stop
  (when repl-server
    (nrepl/stop repl-server)))

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
