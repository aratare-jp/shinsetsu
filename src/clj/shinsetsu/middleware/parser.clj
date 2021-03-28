(ns shinsetsu.middleware.parser
  (:require
    [com.fulcrologic.fulcro.server.api-middleware :as server]))

(defn parser-handler
  "Much like `com.fulcrologic.fulcro.server.api-middleware/wrap-api` but without
  the URI since we can just use reitit instead."
  [parser]
  (when-not (fn? parser)
    (throw (ex-info "Invalid parameters to `wrap-api`. :parser is required. See docstring." {})))
  (fn [request]
    ;; Fulcro's middleware, like ring-transit, places the parsed request in
    ;; the request map on `:transit-params`, other ring middleware, such as
    ;; metosin/muuntaja, places the parsed request on `:body-params`.
    (server/handle-api-request (or (:transit-params request) (:body-params request)) (partial parser {:request request}))))
