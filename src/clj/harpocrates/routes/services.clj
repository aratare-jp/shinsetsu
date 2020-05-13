(ns harpocrates.routes.services
  (:require
    [reitit.ring.coercion :as coercion]
    [reitit.coercion.spec :as spec-coercion]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.multipart :as multipart]
    [reitit.ring.middleware.parameters :as parameters]
    [harpocrates.routes.services.graphql :as graphql]
    [harpocrates.middleware.formats :as formats]
    [harpocrates.middleware.exception :as exception]
    [ring.util.http-response :refer :all]
    [clojure.java.io :as io]
    [harpocrates.spec :as spec]
    [harpocrates.db.core :refer [*db*] :as db]
    [next.jdbc :as jdbc]
    [buddy.hashers :as hashers]))

(defn service-routes []
  ["/api"
   {:coercion   spec-coercion/coercion
    :muuntaja   formats/instance
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 exception/exception-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart
                 multipart/multipart-middleware]}

   ["/graphql" {:post (fn [req]
                        (ok (graphql/execute-request (-> req
                                                         :body
                                                         slurp))))}]

   ["/auth"
    ["/signup"
     {:post {:summary    "signup with user data"
             :parameters {:body {:email      ::spec/email?
                                 :password   string?
                                 :first-name string?
                                 :last-name  string?}}
             :responses  {200 {:headers {:authorization string?}}}
             :handler    (fn [{{{:keys [email
                                        password
                                        first-name
                                        last-name]} :body} :parameters}]
                           (jdbc/with-transaction [t-conn *db*]
                             (if (db/create-user!
                                   t-conn
                                   {:email      email
                                    :password   (hashers/derive password)
                                    :first_name first-name
                                    :last_name  last-name})
                               {:status  200
                                :headers {"Authorization" "test"}}
                               {:status 500})))}}]
    ["/login"
     {:post {:summary    "login with email and password"
             :parameters {:body {:email    ::spec/email?
                                 :password string?}}
             :responses  {200 {:headers {"Authorization" string?}}}
             :handler    (fn [{{{:keys [email password]} :body} :parameters}]
                           (jdbc/with-transaction [t-conn *db*]
                             (let [user (db/get-user-by-email! t-conn {:email email})]
                               (if (and user (hashers/check password (:password user)))
                                 {:status  200
                                  :headers {"Authorization" "testing"}
                                  :body    user}
                                 {:status 404}))))}}]]

   ["/files"
    ["/upload"
     {:post {:summary    "upload a file"
             :parameters {:multipart {:file multipart/temp-file-part}}
             :responses  {200 {:body {:name string?, :size int?}}}
             :handler    (fn [{{{:keys [file]} :multipart} :parameters}]
                           {:status 200
                            :body   {:name (:filename file)
                                     :size (:size file)}})}}]
    ["/download"
     {:get {:summary "downloads a file"
            :swagger {:produces ["image/png"]}
            :handler (fn [_]
                       {:status  200
                        :headers {"Content-Type" "image/png"}
                        :body    (-> "public/img/warning_clojure.png"
                                     (io/resource)
                                     (io/input-stream))})}}]]])
