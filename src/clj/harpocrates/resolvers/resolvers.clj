(ns harpocrates.resolvers.resolvers
  (:require
    [harpocrates.resolvers.user :refer [user-resolver]]
    [harpocrates.resolvers.bookmark :refer [bookmark-resolver]]))

(def resolvers [bookmark-resolver
                user-resolver])
