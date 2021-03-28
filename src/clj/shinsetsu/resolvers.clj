(ns shinsetsu.resolvers
  (:require
    [shinsetsu.resolvers.user :as user]
    [shinsetsu.resolvers.bookmark :as bookmark]
    [shinsetsu.resolvers.tab :as tab]))

(def resolvers (-> []
                   (into user/resolvers)
                   (into bookmark/resolvers)
                   (into tab/resolvers)))
