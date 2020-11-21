(ns harpocrates.mutations.bookmark
  (:require
    [com.wsscode.pathom.connect :as pc]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    #?(:clj [taoensso.timbre :as log])
    #?(:clj [harpocrates.db.bookmark :refer [Bookmark]])
    #?(:clj [toucan.db :as toucan])))

#?(:cljs
   (defmutation create-bookmark
     [_]
     (remote [_] true))
   :clj
   (pc/defmutation create-bookmark
     [_ input]
     {::pc/sym    `create-bookmark
      ::pc/params [:bookmark/url
                   :bookmark/user_id]
      ::pc/output [:bookmark/id]}
     (log/info "Create a new bookmark")
     (let [bookmark (select-keys input [:bookmark/url
                                        :bookmark/user_id])]
       (->> bookmark
            (toucan/insert! Bookmark)
            (:id)))))
