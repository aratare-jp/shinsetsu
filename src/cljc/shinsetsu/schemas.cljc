(ns shinsetsu.schemas
  (:require [schema.core :as s]
            [schema-generators.generators :as g]
            [valip.predicates :as p]
            [clojure.test.check.generators :as check-gen])
  (:import [java.time OffsetDateTime ZoneOffset]
           [javax.sql DataSource]
           [java.sql Connection]))

(defn offset-dt?
  "Check if dt is:

  - an ISO-8601 string with offset of +00:00
  - a java.time.OffsetDateTime instance with offset of +00:00"
  [dt]
  (try
    (if-let [v (condp = (class dt)
                 java.time.OffsetDateTime dt
                 String (OffsetDateTime/parse dt)
                 false)]
      (= ZoneOffset/UTC (.getOffset v))
      false)
    (catch Exception _ false)))

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
   OffsetDT (check-gen/elements
              (repeatedly 100 #(-> (OffsetDateTime/now)
                                   (.withOffsetSameInstant ZoneOffset/UTC))))})

(def Username NonEmptyContinuousStr)

(defn transactable?
  [v]
  (or (instance? DataSource v)
      (instance? Connection v)
      (instance? DataSource (:connectable v))
      (instance? Connection (:connectable v))))

(def Transactable (s/pred transactable?))

(def User
  {:user/id       s/Uuid
   :user/username Username
   :user/password NonEmptyContinuousStr
   :user/image    Bytes})

(def PartialUser
  (-> {}
      (into (map (fn [[k v]] [(s/optional-key k) (s/maybe v)]) User))
      (dissoc :user/id)))

(def UserDB
  (merge User
         {:user/created OffsetDT
          :user/updated OffsetDT}))

(def Session
  {:session/user-id s/Uuid
   :session/expired OffsetDT
   :session/token   NonEmptyContinuousStr})

(def SessionDB
  (merge Session
         {:session/created OffsetDT
          :session/updated OffsetDT}))

(def Tab
  {:tab/id       s/Uuid
   :tab/name     s/Str
   :tab/password s/Str
   :tab/user-id  s/Uuid})

(def PartialTab
  (-> {}
      (into (map (fn [[k v]] [(s/optional-key k) (s/maybe v)]) Tab))
      (dissoc :tab/id)))

(def TabDB
  (merge Tab
         {:tab/created OffsetDT
          :tab/updated OffsetDT}))

(def Bookmark
  {:bookmark/id      s/Uuid
   :bookmark/title   s/Str
   :bookmark/url     s/Str
   :bookmark/image   Bytes
   :bookmark/user-id s/Uuid
   :bookmark/tab-id  s/Uuid})

(def PartialBookmark
  (-> {}
      (into (map (fn [[k v]] [(s/optional-key k) (s/maybe v)]) Bookmark))
      (dissoc :bookmark/id)))

(def BookmarkDB
  (merge Bookmark
         {:bookmark/created OffsetDT
          :bookmark/updated OffsetDT}))

(def Tag
  {:tag/id      s/Uuid
   :tag/name    s/Str
   :tag/colour  s/Str
   :tag/user-id s/Uuid})

(def PartialTag
  (-> {}
      (into (map (fn [[k v]] [(s/optional-key k) (s/maybe v)]) Tag))
      (dissoc :tag/id)))

(def TagDB
  (merge Tag
         {:tag/created OffsetDT
          :tag/updated OffsetDT}))

(def BookmarkTag
  {:bookmark-tag/bookmark-id s/Uuid
   :bookmark-tag/tag-id      s/Uuid})

(def BookmarkTagDB
  (merge BookmarkTag
         {:bookmark-tag/created OffsetDT
          :bookmark-tag/updated OffsetDT}))