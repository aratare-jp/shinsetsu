(ns shinsetsu.schemas
  (:require [schema.core :as s]
            [schema-generators.generators :as g]
            [valip.predicates :as p]
            [clojure.test.check.generators :as check-gen])
  (:import [java.time OffsetDateTime]))

(defn offset-dt?
  "Check if dt is an OffsetDateTime-compatible string or instance."
  [dt]
  (try
    (or (instance? OffsetDateTime dt) (OffsetDateTime/parse dt))
    (catch Exception e false)))

(def OffsetDT (s/pred offset-dt?))
(def Bytes (s/pred bytes?))
(def NonEmptyStr (s/constrained s/Str (complement empty?) "Not empty string"))

(def ContinuousStr
  "String that does not have any whitespace."
  (s/constrained s/Str (complement (partial re-find #"\s")) "Continuous string"))

(def NonEmptyContinuousStr (s/constrained s/Str (every-pred (complement empty?) (complement (partial re-find #"\s")))))

(defn MaxLengthStr [l]
  (s/constrained s/Str #(>= l (count %)) (str "Max length " l)))

(def default-leaf-generator
  {Bytes    check-gen/bytes
   OffsetDT (check-gen/elements (repeatedly 100 #(OffsetDateTime/now)))})

(def User
  {:user/id       s/Uuid
   :user/username NonEmptyContinuousStr
   :user/password NonEmptyContinuousStr
   :user/image    Bytes
   :user/created  OffsetDT
   :user/updated  OffsetDT})

(def CurrentUser
  {:user/id      s/Uuid
   :user/token   NonEmptyContinuousStr
   :user/created OffsetDT
   :user/updated OffsetDT})

(def User? (into {} (map (fn [[k v]] [(s/optional-key k) (s/maybe v)]) User)))

(def Tab
  {:tab/id       s/Uuid
   :tab/name     s/Str
   :tab/password s/Str
   :tab/created  OffsetDT
   :tab/updated  OffsetDT
   :tab/tab-id   s/Uuid})

(def Tab? (into {} (map (fn [[k v]] [(s/optional-key k) (s/maybe v)]) Tab)))

(def Tag
  {:tag/id      s/Uuid
   :tag/name    s/Str
   :tag/colour  (MaxLengthStr 10)
   :tag/image   Bytes
   :tag/created OffsetDT
   :tag/updated OffsetDT
   :tag/tag-id  s/Uuid})

(def Tag? (into {} (map (fn [[k v]] [(s/optional-key k) (s/maybe v)]) Tag)))

(def Bookmark
  {:bookmark/id          s/Uuid
   :bookmark/title       s/Str
   :bookmark/url         s/Str
   :bookmark/image       Bytes
   :bookmark/created     OffsetDT
   :bookmark/updated     OffsetDT
   :bookmark/bookmark-id s/Uuid
   :bookmark/tab-id      s/Uuid})

(def Bookmark? (into {} (map (fn [[k v]] [(s/optional-key k) (s/maybe v)]) Bookmark)))
