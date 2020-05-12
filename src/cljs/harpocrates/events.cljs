(ns harpocrates.events
  (:require
    [reagent.core :as reagent]
    [re-frame.core :as rf]
    [ajax.core :as ajax]
    [day8.re-frame.http-fx]))

(rf/reg-event-db
  :initialise
  (fn [_ _]
    (println "Initialised!")
    {}))

(rf/reg-event-db
  ::login-success
  (fn [db [_ result]]
    (-> db
        (dissoc :login-failed?)
        (assoc-in [:id] (:id result))
        (assoc-in [:blah] "blah")
        (assoc-in [:first-name] (:first-name result))
        (assoc-in [:last-name] (:last-name result))
        (assoc-in [:email] (:email result))
        (assoc-in [:access-token] (:access-token result)))))

(rf/reg-event-db
  ::login-failed
  (fn [db _]
    (-> db
        (assoc-in [:login-failed?] true))))

(rf/reg-event-fx
  :login
  (fn [{:keys [db]} [_ email password]]
    {:http-xhrio {:method          :post
                  :uri             "http://localhost:3000/api/auth/login"
                  :params          {:email    email
                                    :password password}
                  :timeout         5000
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords?
                                                               true})
                  :on-success      [::login-success]
                  :on-failure      [::login-failed]}}))
