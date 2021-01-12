(ns harpocrates.ui.login
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [harpocrates.ui.common :as cm]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(defn on-submit-login
  [{:keys [username password]}]
  (println username)
  (println password)
  (go (let [response (<! (http/post "http://localhost:3000/login" {:form-params {:username username :password password}}))]
        (println (:status response))
        (println (:body response)))))

(defsc login-form
  [_ _]
  {}
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
                {:onClick #(on-submit-login @current-user)} "Login"))))))))

(def ui-login-form (comp/factory login-form))