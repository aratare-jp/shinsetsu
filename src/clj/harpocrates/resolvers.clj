(ns harpocrates.resolvers
  (:require
    [harpocrates.resolvers.user :as user]))

(def resolvers (-> [] (into user/resolvers)))
