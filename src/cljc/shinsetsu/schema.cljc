(ns shinsetsu.schema
  (:require
    [malli.core :as m]
    [malli.error :as me]))

(def non-empty-string (m/schema [:string {:min 1}]))

;; USER

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

;; TAB

(def tab-form-spec
  [:map
   [:tab/name non-empty-string]
   [:tab/password {:optional true} :string]])

(def tab-create-spec
  [:map
   {:closed true}
   [:tab/name non-empty-string]
   [:tab/password {:optional true} non-empty-string]
   [:tab/user-id :uuid]])

(def tab-fetch-spec
  [:map
   {:closed true}
   [:tab/id :uuid]
   [:tab/password {:optional true} :string]
   [:tab/user-id :uuid]])

(def tab-multi-fetch-spec
  [:map
   {:closed true}
   [:tab/user-id :uuid]])

(def tab-patch-spec
  [:map
   {:closed true}
   [:tab/id :uuid]
   [:tab/name {:optional true} non-empty-string]
   [:tab/password {:optional true} non-empty-string]
   [:tab/user-id :uuid]])

(def tab-delete-spec
  [:map
   {:closed true}
   [:tab/id :uuid]
   [:tab/user-id :uuid]])

;; BOOKMARK

(def bookmark-create-spec
  [:map
   {:closed true}
   [:bookmark/title non-empty-string]
   [:bookmark/url non-empty-string]
   [:bookmark/image {:optional true} non-empty-string]
   [:bookmark/tab-id :uuid]
   [:bookmark/user-id :uuid]])

(def bookmark-fetch-spec
  [:map
   {:closed true}
   [:bookmark/id :uuid]
   [:bookmark/user-id :uuid]])

(def bookmark-multi-fetch-spec
  [:map
   {:closed true}
   [:bookmark/tab-id :uuid]
   [:bookmark/user-id :uuid]])

(def bookmark-patch-spec
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

;; TAG

(def hex-colour-regex #"^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")
(defn is-hex-colour-str? [s] (not (nil? (re-find hex-colour-regex s))))
(def hex-colour-spec
  (m/schema [:and
             non-empty-string
             [:fn {:error/message "must have hex colour format"} is-hex-colour-str?]]))

(def tag-create-spec
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

;; BOOKMARK TAG

(def bookmark-tag-create-spec
  [:map
   {:closed true}
   [:bookmark-tag/bookmark-id :uuid]
   [:bookmark-tag/tag-id :uuid]
   [:bookmark-tag/user-id :uuid]])

(def bookmark-tag-fetch-by-bookmark-spec
  [:map
   {:closed true}
   [:bookmark-tag/bookmark-id :uuid]
   [:bookmark-tag/user-id :uuid]])

(def bookmark-tag-fetch-by-tag-spec
  [:map
   {:closed true}
   [:bookmark-tag/tag-id :uuid]
   [:bookmark-tag/user-id :uuid]])

(def bookmark-tag-delete-spec bookmark-tag-create-spec)

(comment
  (println #:foo{:a 1 :foo/b 2})
  (println #:foo{:a 1 :foo/b 2})
  (println #{:foo/a :foo/b})
  (println {:foo/a 1 :foo/b 2}))
