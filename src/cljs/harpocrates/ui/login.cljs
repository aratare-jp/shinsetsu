(ns harpocrates.ui.login
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [harpocrates.ui.elastic-ui :as cm]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [cljs-http.client :as http]
    [cljs.core.async :refer [<!]]
    [harpocrates.vars :refer [token]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :refer [change-route! route-immediate]]))

(defn on-submit-login
  [this {:keys [username password]}]
  (go
    (let [response (<! (http/post "http://localhost:3000/login" {:form-params {:username username :password password}}))]
      (if (= (:status response) 200)
        (do (reset! token (-> response :body :token))
            (change-route! this ["main"]))
        (js/alert "Login unsuccessful")))))

(defsc Login
  [this _]
  {:route-segment ["login"]
   :query         [:login]
   :initial-state {:login {}}
   :ident        [:component/id :login]
   :will-enter    (fn [_ _] (route-immediate [:component/id :login]))}
  (let [current-user (atom {})]
    (cm/ui-page
      nil
      (cm/ui-page-body
        {:component "div"}
        (cm/ui-page-content
          {:verticalPosition   "center"
           :horizontalPosition "center"}
          (cm/ui-page-content-header
            nil
            (cm/ui-page-content-header-section
              nil
              (cm/ui-title
                nil
                (dom/h1 "Login"))))
          (cm/ui-page-content-body
            nil
            (cm/ui-form
              {:component "form"}
              (cm/ui-form-row
                {:label "Username"}
                (cm/ui-field-text
                  {:name     "username"
                   :onChange #(swap! current-user assoc :username (-> % .-target .-value))}))
              (cm/ui-form-row
                {:label "Password"}
                (cm/ui-field-text
                  {:name     "password"
                   :type     "password"
                   :onChange #(swap! current-user assoc :password (-> % .-target .-value))}))
              (cm/ui-button
                {:onClick #(on-submit-login this @current-user)} "Login"))))))))

(def ui-login-form (comp/factory Login))