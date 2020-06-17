(ns harpocrates.mutation
  (:require
    [com.wsscode.pathom.connect :as pc]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [harpocrates.utility :refer [qualify-km]]
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
       (-> (if (:user/id user)
             (update user :user/id #(UUID/fromString %))
             user)
           (#((if (:user/id %) db/create-user-with-id! db/create-user!) db/*db* %))
           (qualify-km "user")
           (select-keys [:user/id])
           (update :user/id #(.toString %))))))

#?(:cljs
   (defmutation create-bookmark
     [_]
     (remote [_] true))
   :clj
   (pc/defmutation create-bookmark
     [_ input]
     {::pc/sym    `create-bookmark
      ::pc/params [:bookmark/id :bookmark/url]
      ::pc/output [:bookmark/id]}
     (log/info "Create a new bookmark")
     (let [bookmark (select-keys input [:bookmark/id
                                        :bookmark/url])]

       (-> (if (:bookmark/id bookmark)
             (update bookmark :bookmark/id #(UUID/fromString %))
             bookmark)
           (#((if (:bookmark/id %) db/create-bookmark-with-id! db/create-bookmark!) db/*db* %))
           (qualify-km "bookmark")
           (select-keys [:bookmark/id])
           (update :bookmark/id #(.toString %))))))

#?(:cljs
   (defmutation create-user-bookmark
     [_]
     (remote [_] true))
   :clj
   (pc/defmutation create-user-bookmark
     [_ input]
     {::pc/sym    `create-user-bookmark
      ::pc/params [:user/id :bookmark/id]
      ::pc/output [:user/id :bookmark/id]}
     (log/info "Linking a bookmark to a user")
     (let [result (-> input (#(db/create-user-bookmark! db/*db* %)))]
       {:user/id     (.toString (:user-id result))
        :bookmark/id (.toString (:bookmark-id result))})))

(def mutations [create-user create-bookmark create-user-bookmark])
