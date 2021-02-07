(ns harpocrates.mutations
  (:require [harpocrates.mutations.user :as user]))

;; File used to aggregate all mutations within the serverside.

(def mutations (-> []
                   (into user/mutations)))
