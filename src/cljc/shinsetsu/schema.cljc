(ns shinsetsu.schema
  (:require
    [malli.core :as m]
    [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
    [com.fulcrologic.fulcro.networking.file-upload :as fu]))

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
   [:tab/password {:optional true} [:maybe :string]]])

(def tab-create-spec
  [:map
   {:closed true}
   [:tab/id {:optional true} [:fn tempid/tempid?]]
   [:tab/name non-empty-string]
   [:tab/password {:optional true} non-empty-string]
   [:tab/user-id :uuid]])

(def tab-fetch-spec
  [:map
   {:closed true}
   [:tab/id :uuid]
   [:tab/password {:optional true} [:maybe :string]]
   [:tab/user-id :uuid]])

(def tab-multi-fetch-spec
  [:map
   {:closed true}
   [:tab/user-id :uuid]
   [:tab/query {:optional true} map?]])

(def tab-patch-spec
  [:map
   {:closed true}
   [:tab/id :uuid]
   [:tab/name {:optional true} non-empty-string]
   [:tab/password {:optional true} [:maybe non-empty-string]]
   [:tab/user-id :uuid]
   [:tab/unlock {:optional true} :any]])

(def tab-delete-spec
  [:map
   {:closed true}
   [:tab/id :uuid]
   [:tab/user-id :uuid]])

;; BOOKMARK

(def bookmark-form-spec
  [:map
   [:bookmark/title non-empty-string]
   [:bookmark/url non-empty-string]])

(def file-spec
  (m/schema
    [:map
     [:filename non-empty-string]
     [:content-type non-empty-string]
     [:tempfile :any]
     [:size :int]]))

(def bookmark-create-spec
  [:map
   {:closed true}
   [:bookmark/id {:optional true} [:fn tempid/tempid?]]
   [:bookmark/title non-empty-string]
   [:bookmark/url non-empty-string]
   [:bookmark/image {:optional true} :any]
   [:bookmark/favourite {:optional true} :boolean]
   [:bookmark/tab-id :uuid]
   [:bookmark/user-id :uuid]
   [::fu/files {:optional true} [:cat file-spec]]])

(def bookmark-fetch-spec
  [:map
   {:closed true}
   [:bookmark/id :uuid]
   [:bookmark/user-id :uuid]])

(def bookmark-bulk-fetch-spec
  [:map
   {:closed true}
   [:bookmark/tab-id :uuid]
   [:bookmark/user-id :uuid]])

(def bookmark-fetch-opts-spec
  [:map
   [:query {:optional true} [:maybe :map]]
   [:sort {:optional true} [:maybe [:vector
                                    [:map
                                     [:field [:enum :bookmark/title :bookmark/created :bookmark/favourite]]
                                     [:direction [:enum :asc :desc]]]]]]
   [:page {:optional true} [:maybe :int]]
   [:size {:optional true} [:maybe :int]]])

(def bookmark-patch-spec
  [:map
   {:closed true}
   [:bookmark/id :uuid]
   [:bookmark/title {:optional true} non-empty-string]
   [:bookmark/url {:optional true} non-empty-string]
   [:bookmark/image {:optional true} :any]
   [:bookmark/favourite {:optional true} :boolean]
   [:bookmark/tab-id {:optional true} :uuid]
   [:bookmark/user-id :uuid]
   [::fu/files {:optional true} [:cat file-spec]]])

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

(def tag-form-spec
  [:map
   [:tag/name non-empty-string]
   [:tag/colour {:optional true} hex-colour-spec]])

(def tag-create-spec
  [:map
   {:closed true}
   [:tag/id {:optional true} [:fn tempid/tempid?]]
   [:tag/name non-empty-string]
   [:tag/colour {:optional true} hex-colour-spec]
   [:tag/user-id :uuid]])

(def tag-fetch-spec
  [:map
   {:closed true}
   [:tag/id :uuid]
   [:tag/user-id :uuid]])

(def tag-multi-fetch-spec
  [:map
   {:closed true}
   [:tag/name {:optional true} [:maybe :string]]
   [:tag/name-pos {:optional true} [:maybe [:enum :start :end :between]]]
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

(def bookmark-tag-multi-create-spec
  [:map
   {:closed true}
   [:bookmark-tag/bookmark-id :uuid]
   [:bookmark-tag/tag-ids [:vector :uuid]]
   [:bookmark-tag/user-id :uuid]])

(def bookmark-tag-fetch-by-bookmark-spec
  [:map
   {:closed true}
   [:bookmark/id :uuid]
   [:bookmark/user-id :uuid]])

(def bookmark-tag-delete-spec bookmark-tag-create-spec)

(def bookmark-tag-multi-delete-spec
  [:map
   {:closed true}
   [:bookmark-tag/bookmark-id :uuid]
   [:bookmark-tag/tag-ids [:vector :uuid]]
   [:bookmark-tag/user-id :uuid]])

(comment
  (user/restart)
  (println #:foo{:a 1 :foo/b 2})
  (println #:foo{:a 1 :foo/b 2})
  (println #{:foo/a :foo/b})
  (println {:foo/a 1 :foo/b 2}))
