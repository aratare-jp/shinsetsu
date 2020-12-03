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
    [harpocrates.db.user :refer [User]]
    [toucan.db :refer [insert!]]
    [buddy.hashers :as hashers]
    [buddy.sign.jwt :as jwt]
    [harpocrates.middleware :as hmid]))

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

   ["/auth"
    ["/signup"
     {:post {:summary    "signup with user data"
             :parameters {:body {:email      ::spec/email?
                                 :password   string?
                                 :first-name string?
                                 :last-name  string?}}
             :handler    (fn [{{{:keys [id
                                        email
                                        password
                                        first-name
                                        last-name]} :body} :parameters}]
                           ; If not exists, throw.
                           (if (User :email email)
                             {:status 401 :message "Email already registered"}
                             (insert! User {:id         id
                                            :email      email
                                            :password   password
                                            :first-name first-name
                                            :last-name  last-name})))}}]
    ["/login"
     {:post {:summary    "login with email and password"
             :parameters {:body {:email    ::spec/email?
                                 :password string?}}
             :handler    (fn [{{{:keys [email password]} :body} :parameters}]
                           (let [unauthed [false {:message "User or password incorrect"}]]
                             (if-let [user (User :email email)]
                               (if (hashers/check password (:password user))
                                 [true {:user (dissoc user :password)}]
                                 unauthed)
                               unauthed)))}}]]

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
