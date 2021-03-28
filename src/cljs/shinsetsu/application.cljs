(ns shinsetsu.application
  (:require
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.networking.http-remote :as http]
    [shinsetsu.db :refer [db]]
    [com.wsscode.pathom.core :as p]))

(defn attach-token
  [handler app]
  (fn [req]
    (if-let [token @db]
      (handler (assoc req :token token))
      (handler req))))

(defn contains-error?
  "Check to see if the response contains Pathom error indicators."
  [body]
  (when (map? body)
    (let [values (vals body)]
      (reduce
        (fn [error? v]
          (if (and (map? v) (:status-code v) (not= (:status-code v) 200))
            (reduced true)
            error?))
        false
        values))))

;; TODO: Global remote error
;:remote-error? (fn [{:keys [body] :as result}]
;                 (or (app/default-remote-error? result)
;                     (contains-error? body)))

(defonce app (app/fulcro-app {:remotes {:remote (http/fulcro-http-remote {:request-middleware
                                                                          (-> (http/wrap-fulcro-request))})}}))