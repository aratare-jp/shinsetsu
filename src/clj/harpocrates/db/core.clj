(ns harpocrates.db.core
  (:require [mount.core :refer [defstate]]))

(defstate ^:dynamic *db*
  :start
  (atom {:user/id {1 {:user/id       1
                      :user/email    "test@test.com"
                      :user/password "bcrypt+sha512$5d367515e8598471c6e089238a9f3d40$12$3bf83ab3192f47760a3d2dfd40108f4bc830ff07a5e4c5da"}}}))
