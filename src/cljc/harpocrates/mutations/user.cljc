(ns harpocrates.mutations.user
  (:require
    [com.wsscode.pathom.connect :as pc]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    #?(:clj [taoensso.timbre :as log])
    #?(:clj [harpocrates.db.user :refer [User]])
    #?(:clj [toucan.db :as toucan])))

#?(:cljs
   (defmutation create-user
     [_]
     (remote [_] true))
   :clj
   (pc/defmutation create-user
     [_ input]
     {::pc/sym    `create-user
      ::pc/params [:user/first-name
                   :user/last-name
                   :user/email
                   :user/password]
      ::pc/output [:user/id]}
     (log/info "Create a new user")
     (let [user (select-keys input [:user/id
                                    :user/first-name
                                    :user/last-name
                                    :user/email
                                    :user/password])]
       (->> user
            (toucan/insert! User)
            (:id)))))
