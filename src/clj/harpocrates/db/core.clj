(ns harpocrates.db.core
  (:require [mount.core :refer [defstate]]))

(defstate ^:dynamic *db*
  :start
  (atom {:user/id {"test@test.com" {:username "test@test.com"
                                    :password "12345"}}}))
