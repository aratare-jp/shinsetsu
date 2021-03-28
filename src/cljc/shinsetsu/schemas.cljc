(ns shinsetsu.schemas
  (:require [schema.core :as s]
            [schema-generators.generators :as g]
            [valip.predicates :as p]
            [clojure.test.check.generators :as check-gen])
  (:import [java.time ZonedDateTime Instant]))

(defn instant?
  [v]
  (try
    (or
      (instance? Instant v)
      (Instant/parse v))
    (catch Exception _ false)))

(def IInstant (s/pred instant?))
(def Bytes (s/pred bytes?))
(def NonEmptyStr (s/constrained s/Str (complement empty?) "Not empty string"))

(def ContinuousStr
  "String that does not have any whitespace."
  (s/constrained s/Str (complement (partial re-find #"\s")) "Continuous string"))

(def NonEmptyContinuousStr (s/constrained s/Str (every-pred (complement empty?) (complement (partial re-find #"\s")))))

(defn MaxLengthStr [l]
  (s/constrained s/Str #(>= l (count %)) (str "Max length " l)))

(def default-leaf-generator
  {IInstant (check-gen/elements (doall (repeatedly 100 #(Instant/now))))
   Bytes    check-gen/bytes})

(def User
  {:user/id       s/Uuid
   :user/username NonEmptyContinuousStr
   :user/password NonEmptyContinuousStr
   :user/image    Bytes
   :user/created  IInstant
   :user/updated  IInstant})

(def CurrentUser
  {:user/id      s/Uuid
   :user/token   NonEmptyContinuousStr
   :user/created IInstant
   :user/updated IInstant})

(def User? (into {} (map (fn [[k v]] [(s/optional-key k) (s/maybe v)]) User)))

(def Tab
  {:tab/id       s/Uuid
   :tab/name     s/Str
   :tab/password s/Str
   :tab/created  IInstant
   :tab/updated  IInstant
   :tab/tab-id   s/Uuid})

(def Tab? (into {} (map (fn [[k v]] [(s/optional-key k) (s/maybe v)]) Tab)))

(def Tag
  {:tag/id      s/Uuid
   :tag/name    s/Str
   :tag/colour  (MaxLengthStr 10)
   :tag/image   Bytes
   :tag/created IInstant
   :tag/updated IInstant
   :tag/tag-id  s/Uuid})

(def Tag? (into {} (map (fn [[k v]] [(s/optional-key k) (s/maybe v)]) Tag)))

(def Bookmark
  {:bookmark/id          s/Uuid
   :bookmark/title       s/Str
   :bookmark/url         s/Str
   :bookmark/image       Bytes
   :bookmark/created     IInstant
   :bookmark/updated     IInstant
   :bookmark/bookmark-id s/Uuid
   :bookmark/tab-id      s/Uuid})

(def Bookmark? (into {} (map (fn [[k v]] [(s/optional-key k) (s/maybe v)]) Bookmark)))
