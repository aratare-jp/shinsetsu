(ns ^:figwheel-hooks harpocrates.core
  (:require
    [re-frame.core :as rfc]
    [reagent.core :as reagent :refer [atom]]
    [reagent.dom :as rdom]
    [reagent.session :as session]
    [reitit.frontend :as reitit]
    [clerk.core :as clerk]
    [accountant.core :as accountant]
    [harpocrates.events]))

;; -------------------------
;; States
(def is-logged-in (atom false))
(def email (atom ""))
(def password (atom ""))

;; -------------------------
;; Routes

(def router
  (reitit/router
    [["/login" :login]
     ["/" :index]
     ["/items"
      ["" :items]
      ["/:item-id" :item]]
     ["/about" :about]]))

(defn path-for [route & [params]]
  (if params
    (:path (reitit/match-by-name router route params))
    (:path (reitit/match-by-name router route))))


;; -------------------------
;; Page components

(defn home-page []
  (fn []
    [:span.main
     [:h1 "Welcome to reagent-noobies"]
     [:ul
      [:li [:a {:href (path-for :items)} "Items of reagent-noobies"]]
      [:li [:a {:href "/broken/link"} "Broken link"]]]
     [:input]]))


(defn items-page []
  (fn []
    [:span.main
     [:h1 "The items of reagent-noobies"]
     [:ul (map (fn [item-id]
                 [:li {:name (str "item-" item-id) :key (str "item-" item-id)}
                  [:a {:href (path-for :item {:item-id item-id})} "Item: " item-id]])
               (range 1 60))]]))


(defn item-page []
  (fn []
    (let [routing-data (session/get :route)
          item         (get-in routing-data [:route-params :item-id])]
      [:span.main
       [:h1 (str "Item " item " of reagent-noobies")]
       [:p [:a {:href (path-for :items)} "Back to the list of items"]]])))


(defn about-page []
  (fn []
    [:span.main
     [:h1 "About Harpocrates"]]))


(defn login-page []
  (fn []
    [:div
     [:h1 "Login"]
     [:form
      [:div.form-group
       [:label {:for "email-address"} "Email Address"]
       [:input#email-address.form-control
        {:type             "email"
         :aria-describedby "emailHelp"
         :value            @email
         :on-change        #(reset! email (-> % .-target .-value))}]
       [:small#email-help.form-text.text-muted
        "We'll never share your email address with other parties.
       "]]
      [:div.form-group
       [:label {:for "password"} "Password"]
       [:input#password.form-control
        {:type             "password"
         :aria-describedby "passwordHelp"
         :value            @password
         :on-change        #(reset! password (-> % .-target .-value))}]
       [:small#password-help.form-text.text-muted
        "We'll never share your password address with other parties.
       "]]
      [:div.btn-group {:role "group" :aria-label "Button group"}
       [:button
        {:type     "button"
         :class    "btn btn-primary"
         :on-click #(rfc/dispatch [:login @email @password])}
        "Login"]
       [:button
        {:type     "button"
         :class    "btn btn-secondary"
         :on-click (fn []
                     (reset! email "")
                     (reset! password ""))}
        "Clear"]]]]))

;; -------------------------
;; Translate routes -> page components

(defn page-for [route]
  (case route
    :login #'login-page
    :index #'home-page
    :about #'about-page
    :items #'items-page
    :item #'item-page
    #'home-page))

;; -------------------------
;; Page mounting component

(defn current-page []
  (fn []
    (let [page (:current-page (session/get :route))]
         [:div
          [page]])))


;; -------------------------
;; Initialize app

(defn mount-root []
  (rdom/render [#'current-page] (.getElementById js/document "app")))

;; Figwheel main hook used to rerender reagent after file changes.
(defn ^:after-load re-render []
  (mount-root))

(defn init! []
  (rfc/dispatch [:initialise])
  (clerk/initialize!)
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (let [match        (reitit/match-by-path router path)
             current-page (:name (:data match))
             route-params (:path-params match)]
         (if (and (not @is-logged-in) (not= :login current-page))
           (accountant/navigate! :login))
         (reagent/after-render clerk/after-render!)
         (session/put! :route {:current-page (page-for current-page)
                               :route-params route-params})
         (clerk/navigate-page! path)))
     :path-exists?
     (fn [path]
       (boolean (reitit/match-by-path router path)))})
  ;; If not logged in, redirect
  (if @is-logged-in
    (accountant/dispatch-current!)
    (accountant/navigate! "/login"))
  (mount-root))
