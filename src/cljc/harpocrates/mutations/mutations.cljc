(ns harpocrates.mutations.mutations
  (:require
    [harpocrates.mutations.user :refer [create-user]]
    [harpocrates.mutations.bookmark :refer [create-bookmark]]))

(def mutations [create-user create-bookmark])
