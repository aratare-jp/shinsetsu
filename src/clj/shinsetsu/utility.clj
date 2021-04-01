(ns shinsetsu.utility
  (:require [schema.core :as s])
  (:import [java.time ZoneOffset OffsetDateTime]))

(s/defn now :- OffsetDateTime
  "Return a new java.time.OffsetDateTime instance with offset +00:00 (UTC)"
  []
  (-> (OffsetDateTime/now) (.withOffsetSameInstant ZoneOffset/UTC)))

(s/defn simplify-kw :- s/Any
  "Given a map and a namespace, convert all keys within the map to fully qualified keywords with
  the given namespace.

  For example:
  ```
  (simplify {:foo/a 1 :foo/b 2 :bar/3} \"baz\")
  => {:baz/1 :baz/2 :baz/3}
  ```
  "
  [m ns]
  (into {} (for [[k v] m] [(->> k name (keyword ns)) v])))
