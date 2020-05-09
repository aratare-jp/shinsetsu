(ns harpocrates.core-test
  (:require [cljs.test :refer-macros [is are deftest testing use-fixtures]]
            [pjstadig.humane-test-output]
            [reagent.core :as reagent :refer [atom]]
            [harpocrates.core :as rc]))

(deftest test-home
  (is (= "1" "2")))

(deftest blah
  (is (= 1 1)))

(deftest blue
  (is (= 1 1)))
