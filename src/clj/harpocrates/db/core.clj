(ns harpocrates.db.core
  (:require [mount.core :refer [defstate]]))

(defstate ^:dynamic *db*
  :start
  (atom {:user/id     {1 {:user/id       1
                          :user/email    "test@test.com"
                          :user/password "bcrypt+sha512$5d367515e8598471c6e089238a9f3d40$12$3bf83ab3192f47760a3d2dfd40108f4bc830ff07a5e4c5da"
                          :user/tabs     [{:tab/id 1}
                                          {:tab/id 2}]}}
         :tab/id      {1 {:tab/id        1
                          :tab/name      "Home"
                          :tab/bookmarks [{:bookmark/id 1}]}
                       2 {:tab/id        2
                          :tab/name      "House"
                          :tab/bookmarks [{:bookmark/id 1}
                                          {:bookmark/id 2}]}}
         :bookmark/id {1 {:bookmark/id  1
                          :bookmark/url "https://theverge.com"}
                       2 {:bookmark/id  2
                          :bookmark/url "https://theregister.com"}}}))