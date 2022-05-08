(ns shinsetsu.application
  (:require
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.networking.file-upload :as fu]
    [com.fulcrologic.fulcro.networking.http-remote :as http]
    [shinsetsu.store :refer [get-key set-key store]]))

(defn wrap-url
  [handler extra-paths]
  (fn [req]
    (handler (assoc req :url (str (get-key @store :remoteUrl) extra-paths)))))

(defn wrap-auth-token
  [handler]
  (fn [req]
    (handler (assoc-in req [:headers "Authorization"] (str "Bearer " (get-key @store :userToken))))))

(def remote-middleware
  (-> (http/wrap-fulcro-request)
      (fu/wrap-file-upload)
      (wrap-auth-token)
      (wrap-url "/api")))

(def auth-middleware
  (-> (http/wrap-fulcro-request)
      (wrap-url "/auth")))

(defn contain-errors?
  "Recursively traverse down the pathom path and check if the given Pathom response is an error response."
  [it]
  (cond
    (map? it) (or (:error it) (some (fn [v] (contain-errors? (second v))) it))
    (coll? it) (some #(contain-errors? %) it)
    :else false))

(defonce app (app/fulcro-app {:remote-error? (fn [{:keys [body] :as result}]
                                               (or (app/default-remote-error? result) (contain-errors? body)))
                              :remotes
                              {:auth   (http/fulcro-http-remote {:request-middleware auth-middleware})
                               :remote (http/fulcro-http-remote {:request-middleware remote-middleware})}}))
