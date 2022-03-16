(ns shinsetsu.schema
  (:require
    [malli.core :as m]
    [malli.error :as me]))

(def valid-id? (m/validator :uuid))
(def non-empty-string (m/schema [:string {:min 1}]))

(def user-spec
  [:map
   {:closed true}
   [:user/id :uuid]
   [:user/username non-empty-string]
   [:user/password non-empty-string]
   [:user/created {:optional true} inst?]
   [:user/updated {:optional true} inst?]])

(def valid-user? (m/validator user-spec))

(def tab-spec
  [:map
   {:closed true}
   [:tab/id {:optional true} :uuid]
   [:tab/name non-empty-string]
   [:tab/password {:optional true} non-empty-string]
   [:tab/created {:optional true} inst?]
   [:tab/updated {:optional true} inst?]
   [:tab/user-id :uuid]])

(def valid-tab? (m/validator tab-spec))

(comment
  (m/explain inst? (java.time.Instant/now))
  (let [uuid (java.util.UUID/randomUUID)
        user {:user/id       uuid
              :user/username "boo"
              :user/password "bar"
              :user/created  (java.time.Instant/now)}]
    (me/humanize (m/explain user-spec user)))
  (m/validate non-empty-string nil))
