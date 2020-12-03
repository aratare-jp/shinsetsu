(ns harpocrates.db.shared
  (:require [toucan.models :refer [add-property! add-type!]]
            [buddy.hashers :as hs])
  (:import (java.sql Timestamp)
           (java.util UUID)))

(add-property!
  :timestamped?
  :insert (fn [obj _]
            (let [now (Timestamp. (System/currentTimeMillis))]
              (assoc obj :created-at now, :updated-at now)))
  :update (fn [obj _]
            (assoc obj :updated-at (Timestamp. (System/currentTimeMillis)))))

(add-property!
  :uuid?
  :insert (fn [obj _]
            (if-let [id (try
                          (UUID/fromString (:id obj))
                          (catch Exception _ false))]
              (update obj :id id)
              (update obj :id (UUID/randomUUID)))))

(add-type!
  :tc-uuid
  :in #(UUID/fromString %)
  :out #(.toString %))

(add-type!
  :tc-password
  :in #(hs/encrypt %))
