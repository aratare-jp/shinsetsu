(ns shinsetsu.schema
  (:require
    [malli.core :as m]
    [malli.error :as me]))

(def non-empty-string (m/schema [:string {:min 1}]))

(def user-spec
  [:map
   {:closed true}
   [:user/username non-empty-string]
   [:user/password non-empty-string]])

(def user-update-spec
  [:map
   {:closed true}
   [:user/id :uuid]
   [:user/username {:optional true} non-empty-string]
   [:user/password {:optional true} non-empty-string]])

(def tab-form-spec
  [:map
   [:tab/name non-empty-string]
   [:tab/password {:optional true} non-empty-string]
   [:tab/is-protected? {:optional true} :boolean]])

(def tab-spec
  [:map
   {:closed true}
   [:tab/name non-empty-string]
   [:tab/password {:optional true} non-empty-string]
   [:tab/is-protected? {:optional true} :boolean]
   [:tab/user-id :uuid]])

(def tab-update-spec
  [:map
   {:closed true}
   [:tab/id :uuid]
   [:tab/name {:optional true} non-empty-string]
   [:tab/password {:optional true} non-empty-string]
   [:tab/is-protected? {:optional true} :boolean]
   [:tab/user-id :uuid]])

(def tab-delete-spec
  [:map
   {:closed true}
   [:tab/id :uuid]
   [:tab/user-id :uuid]])

(def bookmark-spec
  [:map
   {:closed true}
   [:bookmark/title non-empty-string]
   [:bookmark/url non-empty-string]
   [:bookmark/image {:optional true} non-empty-string]
   [:bookmark/tab-id :uuid]
   [:bookmark/user-id :uuid]])

(def bookmark-update-spec
  [:map
   {:closed true}
   [:bookmark/id :uuid]
   [:bookmark/title {:optional true} non-empty-string]
   [:bookmark/url {:optional true} non-empty-string]
   [:bookmark/image {:optional true} non-empty-string]
   [:bookmark/tab-id {:optional true} :uuid]
   [:bookmark/user-id :uuid]])

(def bookmark-delete-spec
  [:map
   {:closed true}
   [:bookmark/id :uuid]
   [:bookmark/user-id :uuid]])

(def hex-colour-regex #"^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")
(defn is-hex-colour-str? [s] (not (nil? (re-find hex-colour-regex s))))
(def hex-colour-spec (m/schema [:and
                                non-empty-string
                                [:fn {:error/message "must have hex colour format"} is-hex-colour-str?]]))

(def tag-spec
  [:map
   {:closed true}
   [:tag/name non-empty-string]
   [:tag/colour {:optional true} hex-colour-spec]
   [:tag/user-id :uuid]])

(def tag-update-spec
  [:map
   {:closed true}
   [:tag/id :uuid]
   [:tag/name {:optional true} non-empty-string]
   [:tag/colour {:optional true} hex-colour-spec]
   [:tag/user-id :uuid]])

(def tag-delete-spec
  [:map
   {:closed true}
   [:tag/id :uuid]
   [:tag/user-id :uuid]])

(def bookmark-tag-spec
  [:map
   {:closed true}
   [:bookmark/id :uuid]
   [:tag/id :uuid]
   [:user/id :uuid]])

(def bookmark-tag-delete-spec bookmark-tag-spec)

(comment
  (me/humanize (m/explain non-empty-string ""))
  (me/humanize (m/explain hex-colour-spec ""))
  (m/explain inst? (java.time.Instant/now))
  (let [uuid (java.util.UUID/randomUUID)
        user {:user/id       uuid
              :user/username "boo"
              :user/password "bar"
              :user/created  (java.time.Instant/now)}]
    (me/humanize (m/explain user-spec user)))
  (m/validate non-empty-string nil))
