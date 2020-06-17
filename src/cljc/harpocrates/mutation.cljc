(ns harpocrates.mutation
  (:require
    [com.wsscode.pathom.connect :as pc]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    #?(:clj [taoensso.timbre :as log])
    #?(:clj [harpocrates.db.core :as db]))
  (:import (java.util UUID)))

#?(:cljs
   (defmutation create-user
     [_]
     (remote [_] true))
   :clj
   (pc/defmutation create-user
     [_ input]
     {::pc/sym `create-user}
     (log/info "Create a new user")
     (db/create-user! db/*db* (-> (select-keys input [:user/id
                                                      :user/first-name
                                                      :user/last-name
                                                      :user/email
                                                      :user/password])
                                  (update :user/id #(UUID/fromString %))))))

(def mutations [create-user])
