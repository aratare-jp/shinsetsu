(ns harpocrates.db.core
  (:require [mount.core :refer [defstate]]))

(defstate ^:dynamic *db*
  :start
  (atom {:user/id     {"25c7aee2-1c95-4a58-a5cf-5b5a9fac7d52"
                       {:user/id       "25c7aee2-1c95-4a58-a5cf-5b5a9fac7d52"
                        :user/email    "test@test.com"
                        :user/password "bcrypt+sha512$5d367515e8598471c6e089238a9f3d40$12$3bf83ab3192f47760a3d2dfd40108f4bc830ff07a5e4c5da"
                        :user/tabs     [{:tab/id "1a6c2c74-e0d3-4d4e-8dce-765e35488071"}
                                        {:tab/id "0a5d7852-004b-403d-b773-601a73353c29"}
                                        {:tab/id "cda029e3-cceb-4ceb-8c51-078b813e4c0e"}]}}

         :tab/id      {"1a6c2c74-e0d3-4d4e-8dce-765e35488071"
                       {:tab/id        "1a6c2c74-e0d3-4d4e-8dce-765e35488071"
                        :tab/name      "Home"
                        :tab/bookmarks [{:bookmark/id "f84b5489-2f0c-4fd3-94dc-ec77683370df"}
                                        {:bookmark/id "c05914ff-c238-4e86-a7c0-1d8c6960e803"}
                                        {:bookmark/id "0f4ab23c-d651-4998-987f-4cbf4670f905"}
                                        {:bookmark/id "c9167468-ccba-4fd3-9a65-93912d96bb70"}
                                        {:bookmark/id "d318b0d3-38ca-42f1-92a0-e25df2d1b396"}
                                        {:bookmark/id "b46aa8a1-f762-42b0-9269-b4de462ef059"}
                                        {:bookmark/id "ca1f985d-57a6-480e-abd4-73a0c58d58a1"}]}

                       "0a5d7852-004b-403d-b773-601a73353c29"
                       {:tab/id        "0a5d7852-004b-403d-b773-601a73353c29"
                        :tab/name      "House"
                        :tab/bookmarks [{:bookmark/id "f84b5489-2f0c-4fd3-94dc-ec77683370df"}
                                        {:bookmark/id "c05914ff-c238-4e86-a7c0-1d8c6960e803"}]}

                       "cda029e3-cceb-4ceb-8c51-078b813e4c0e"
                       {:tab/id        "cda029e3-cceb-4ceb-8c51-078b813e4c0e"
                        :tab/name      "Hut"
                        :tab/bookmarks [{:bookmark/id "f84b5489-2f0c-4fd3-94dc-ec77683370df"}
                                        {:bookmark/id "c05914ff-c238-4e86-a7c0-1d8c6960e803"}
                                        {:bookmark/id "0f4ab23c-d651-4998-987f-4cbf4670f905"}]}}

         :bookmark/id {"f84b5489-2f0c-4fd3-94dc-ec77683370df"
                       {:bookmark/id   "f84b5489-2f0c-4fd3-94dc-ec77683370df"
                        :bookmark/name "The Verge"
                        :bookmark/url  "https://theverge.com"}

                       "c05914ff-c238-4e86-a7c0-1d8c6960e803"
                       {:bookmark/id   "c05914ff-c238-4e86-a7c0-1d8c6960e803"
                        :bookmark/name "The Verge"
                        :bookmark/url  "https://theregister.com"}

                       "0f4ab23c-d651-4998-987f-4cbf4670f905"
                       {:bookmark/id   "0f4ab23c-d651-4998-987f-4cbf4670f905"
                        :bookmark/name "The Verge"
                        :bookmark/url  "https://theregister.com"}

                       "c9167468-ccba-4fd3-9a65-93912d96bb70"
                       {:bookmark/id   "c9167468-ccba-4fd3-9a65-93912d96bb70"
                        :bookmark/name "The Verge"
                        :bookmark/url  "https://theregister.com"}

                       "d318b0d3-38ca-42f1-92a0-e25df2d1b396"
                       {:bookmark/id   "d318b0d3-38ca-42f1-92a0-e25df2d1b396"
                        :bookmark/name "The Verge"
                        :bookmark/url  "https://theregister.com"}

                       "b46aa8a1-f762-42b0-9269-b4de462ef059"
                       {:bookmark/id   "b46aa8a1-f762-42b0-9269-b4de462ef059"
                        :bookmark/name "The Verge"
                        :bookmark/url  "https://theregister.com"}

                       "ca1f985d-57a6-480e-abd4-73a0c58d58a1"
                       {:bookmark/id   "ca1f985d-57a6-480e-abd4-73a0c58d58a1"
                        :bookmark/name "The Verge"
                        :bookmark/url  "https://theregister.com"}}}))