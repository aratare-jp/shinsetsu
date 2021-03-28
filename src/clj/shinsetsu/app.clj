(ns shinsetsu.app
  (:require
    [mount.core :refer [defstate]]
    [reitit.ring :as rr]
    [reitit.ring.coercion :as rrc]
    [reitit.ring.middleware.multipart :as multipart]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.parameters :as parameters]
    [ring.middleware.reload :refer [wrap-reload]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [ring.middleware.defaults :refer [wrap-defaults]]
    [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
    [ring.middleware.gzip :refer [wrap-gzip]]
    [shinsetsu.routers.api :refer [api-routes]]
    [shinsetsu.config :refer [env]]
    [shinsetsu.middleware.exception :as exception]
    [ring.middleware.session :refer [wrap-session]]
    [hiccup.page :as h.page]
    [taoensso.timbre :as timbre]
    [ring.util.response :as response]
    [clojure.string :as str]))

(defn generate-index [{:keys [anti-forgery-token]}]
  "Dynamically generate the index.html so we can embed CSRF nicely."
  (timbre/info "Embedding CSRF token in index page")
  (-> (h.page/html5 {}
                    [:head {:lang "en"}
                     [:meta {:charset "UTF-8"}]
                     [:link {:href "/css/main.css" :rel "stylesheet"}]]
                    [:body
                     [:div#app]
                     [:script {:src "/js/main/app.js"}]])
      response/response
      ;; FIXME: Need to make this a proper token
      (response/header "X-CSRF-Token" "hello-world!")
      (response/content-type "text/html")))

(defn wrap-uris
  "Wrap the given request URIs to a generator function."
  [handler uri-map]
  (fn [{:keys [uri] :as req}]
    (if-let [generator (get uri-map uri)]
      (generator req)
      (handler req))))

(defn all-routes-to-index [handler]
  (fn [{:keys [uri] :as req}]
    (if (or
          (= "/api" uri)
          (str/ends-with? uri ".css")
          (str/ends-with? uri ".map")
          (str/ends-with? uri ".jpg")
          (str/ends-with? uri ".png")
          (str/ends-with? uri ".js"))
      (handler req)
      (handler (assoc req :uri "/index.html")))))

(defstate app
  :start
  (rr/ring-handler
    (rr/router
      [api-routes]
      {:data {:middleware [rrc/coerce-exceptions-middleware
                           rrc/coerce-request-middleware
                           rrc/coerce-response-middleware
                           exception/exception-middleware]}})
    (rr/routes
      (rr/create-resource-handler {:path "/"})
      (rr/create-default-handler))
    {:middleware [(if (:dev? env) wrap-reload)
                  wrap-session
                  ;; FIXME: Enable anti-forgery later.
                  [wrap-defaults {:security {:anti-forgery         false
                                             :xss-protection       {:enable? true :mode :block}
                                             :frame-options        :sameorigin
                                             :content-type-options :nosniff}}]
                  parameters/parameters-middleware
                  muuntaja/format-middleware
                  multipart/multipart-middleware
                  all-routes-to-index
                  wrap-gzip]}))
