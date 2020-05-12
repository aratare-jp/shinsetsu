(ns harpocrates.dev-middleware
  (:require
    [ring.middleware.cors :refer [wrap-cors]]
    [ring.middleware.reload :refer [wrap-reload]]
    [selmer.middleware :refer [wrap-error-page]]
    [prone.middleware :refer [wrap-exceptions]]))

(defn wrap-dev [handler]
  (-> handler
      (wrap-cors
        :access-control-allow-origin #".*"
        :access-control-allow-headers #{:accept :content-type}
        :access-control-allow-methods [:get :put :post :delete])
      wrap-reload
      wrap-error-page
      (wrap-exceptions {:app-namespaces ['harpocrates]})))
