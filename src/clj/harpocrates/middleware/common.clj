(ns harpocrates.middleware.common)

(def not-found-handler
  (fn [req]
    {:status  404
     :headers {"Content-Type" "text/plain"}
     :body    "Not Found"}))
