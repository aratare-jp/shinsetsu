(ns harpocrates.ui.login
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [harpocrates.ui.common :as cm]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [harpocrates.mutations.storage :as storage]))

(defn on-submit-login
  [this {:keys [username password]}]
  (go
    (let [response (<! (http/post "http://localhost:3000/login" {:form-params {:username username :password password}}))]
      (comp/transact! this [(storage/set-token (-> response :body :token))]))))

(defsc login-form
  [this _]
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
                {:onClick #(on-submit-login this @current-user)} "Login"))))))))

(def ui-login-form (comp/factory login-form))