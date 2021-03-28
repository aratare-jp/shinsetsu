(ns shinsetsu.schemas
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

(def User
  {:user/id       s/Uuid
   :user/username NonEmptyContinuousStr
   :user/password NonEmptyContinuousStr
   :user/created  s/Inst
   :user/updated  s/Inst})

(def CurrentUser
  {:user/id      s/Uuid
   :user/token   NonEmptyContinuousStr
   :user/created s/Inst
   :user/updated s/Inst})

(def User? (into {} (map (fn [[k v]] [(s/optional-key k) (s/maybe v)]) User)))

(def Tab
  {:tab/id       s/Uuid
   :tab/name     s/Str
   :tab/password s/Str
   :tab/created  s/Inst
   :tab/updated  s/Inst
   :tab/tab-id   s/Uuid})

(def Tab? (into {} (map (fn [[k v]] [(s/optional-key k) (s/maybe v)]) Tab)))

(def Tag
  {:tag/id      s/Uuid
   :tag/name    s/Str
   :tag/colour  (MaxLengthStr 10)
   :tag/image   s/Any
   :tag/created s/Inst
   :tag/updated s/Inst
   :tag/tag-id  s/Uuid})

(def Tag? (into {} (map (fn [[k v]] [(s/optional-key k) (s/maybe v)]) Tag)))

(def Bookmark
  {:bookmark/id          s/Uuid
   :bookmark/title       s/Str
   :bookmark/url         s/Str
   :bookmark/image       s/Any
   :bookmark/created     s/Inst
   :bookmark/updated     s/Inst
   :bookmark/bookmark-id s/Uuid
   :bookmark/tab-id      s/Uuid})

(def Bookmark? (into {} (map (fn [[k v]] [(s/optional-key k) (s/maybe v)]) Bookmark)))