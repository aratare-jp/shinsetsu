(ns shinsetsu.server
  (:require
    [org.httpkit.server :as http]
    [mount.core :refer [defstate start start-with-args stop]]
    [clojure.tools.cli :as cli]
    [taoensso.timbre :as log]
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

(defn stop-app
  "Stops everything and shutdown the web app."
  []
  (doseq [component (:stopped (stop))]
    (log/info component "stopped"))
  (shutdown-agents))

(defn start-app
  "Initialise everything and start the web app."
  ([] (start-app {}))
  ([args]
   (doseq [component (-> args
                         (cli/parse-opts cli-options)
                         start-with-args
                         :started)]
     (log/info component "started"))
   (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app))))

(defn -main [& args]
  (start #'shinsetsu.config/env)
  (start-app args))

(comment
  (require '[user])
  (user/start)
  {:a 1 :b 2 :c {:d 3}}
  (+ 1 2))
