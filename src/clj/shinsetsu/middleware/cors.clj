(ns shinsetsu.middleware.cors)

(defn wrap-cors
  [handler]
  (fn [req]
    (-> (handler req)
        (assoc-in [:headers "Access-Control-Allow-Origin"] "*")
        (assoc-in [:headers "Access-Control-Allow-Headers"] "*"))))
