(ns shinsetsu.routing
  (:require
    [shinsetsu.application :refer [app]]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [clojure.string :as str]
    [pushy.core :as pushy]))

(defonce history (pushy/pushy
                   (fn [p]
                     (let [route-segments (vec (rest (str/split p "/")))]
                       (log/spy :info route-segments)
                       (dr/change-route app route-segments)))
                   identity))

(defn start! []
  (pushy/start! history))

(defn route-to!
  "Change routes to the given route-string (e.g. \"/home\"."
  [route-string]
  (pushy/set-token! history route-string))
