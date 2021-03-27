(ns harpocrates.spec
  (:require [schema.core :as s]
            [schema-generators.generators :as g]
            [valip.predicates :as p])
  (:import [java.time ZonedDateTime]))

(defn zoned-date-time?
  [v]
  (try
    (ZonedDateTime/parse v)
    (catch Exception _ false)))

(def ZDateTime (s/pred zoned-date-time?))
(def Bytes (s/pred bytes?))
(def Email (s/pred p/email-address?))
(def NonEmptyStr (s/constrained s/Str (complement empty?) "Not empty string"))

(def ContinuousStr
  "String that does not have any whitespace."
  (s/constrained s/Str (complement (partial re-find #"\s")) "Continuous string"))

(def NonEmptyContinuousStr (s/constrained s/Str (every-pred (complement empty?) (complement (partial re-find #"\s")))))

(defn MaxLengthStr [l]
  (s/constrained s/Str #(>= l (count %)) (str "Max length " l)))

(comment
  (s/defn foo [s :- (MaxLengthStr 3)]
    (println s))
  (foo "1111")
  (g/sample 10 (MaxLengthStr 3)))