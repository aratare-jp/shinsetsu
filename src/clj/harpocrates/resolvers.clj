(ns harpocrates.resolvers
  (:require
    [harpocrates.resolvers.user :as user]
    [harpocrates.resolvers.bookmark :as bookmark]
    [harpocrates.resolvers.tab :as tab]))

(def resolvers (-> []
                   (into user/resolvers)
                   (into bookmark/resolvers)
                   (into tab/resolvers)))
