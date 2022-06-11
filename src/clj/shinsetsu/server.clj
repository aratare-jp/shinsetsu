(ns shinsetsu.server
  (:require
    [aleph.http :as http]
    [mount.core :as m]
    [clojure.tools.cli :as cli]
    [taoensso.timbre :as log]
    [shinsetsu.config :refer [env]]
    [shinsetsu.app :refer [app]]
    [shinsetsu.nrepl :as nrepl])
  (:gen-class))

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

(m/defstate ^{:on-reload :noop} http-server
  :start
  (reset! server (http/start-server app (assoc env :port (or (-> env :options :port) 3000))))
  :stop
  (when @server
    (.close @server)
    (reset! server nil)))

(m/defstate ^{:on-reload :noop} repl-server
  :start
  (when (:nrepl-port env)
    (nrepl/start {:bind (:nrepl-bind env)
                  :port (:nrepl-port env)}))
  :stop
  (when repl-server
    (nrepl/stop repl-server)))

(defn stop-app
  "Stops everything and shutdown the web app."
  []
  (doseq [component (:stopped (m/stop))]
    (log/info component "stopped"))
  (shutdown-agents))

(defn start-app
  "Initialise everything and start the web app."
  ([] (start-app {}))
  ([args]
   (m/start #'shinsetsu.config/env)
   (doseq [component (-> args
                         (cli/parse-opts cli-options)
                         m/start-with-args
                         :started)]
     (log/info component "started"))))

(defn -main [& args]
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app))
  (start-app args))

(comment
  (require '[user])
  (user/restart)
  {:a 1 :b 2 :c {:d 3}}
  (+ 1 2))
