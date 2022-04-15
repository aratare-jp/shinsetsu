(ns shinsetsu.application
  (:require
    [com.fulcrologic.fulcro.application :as app]
    [shinsetsu.store :refer [store get-key set-key]]
    [com.fulcrologic.fulcro.networking.http-remote :as http]
    [com.fulcrologic.fulcro.networking.file-upload :as fu]))

(defn wrap-auth-token
  ([handler store]
   (fn [req]
     (handler (assoc-in req [:headers "Authorization"] (str "Bearer " (get-key @store :userToken)))))))

(def req-middleware
  (-> (http/wrap-fulcro-request)
      fu/wrap-file-upload
      (wrap-auth-token store)))

(defn contain-errors?
  [body]
  (let [values (vals body)]
    (reduce (fn [acc {:keys [error]}]
              (if error
                (reduced true)
                acc))
            false
            values)))

(defonce app (app/fulcro-app {:remote-error? (fn [{:keys [body] :as result}]
                                               (or (app/default-remote-error? result)
                                                   (contain-errors? body)))
                              :remotes
                              {:auth   (http/fulcro-http-remote {:url "auth"})
                               :remote (http/fulcro-http-remote {:request-middleware req-middleware})}}))
