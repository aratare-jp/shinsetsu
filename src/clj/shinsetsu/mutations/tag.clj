(ns shinsetsu.mutations.tag
  (:require
    [shinsetsu.db.tag :as db]
    [com.wsscode.pathom.connect :as pc :refer [defmutation]]
    [taoensso.timbre :as log]
    [malli.core :as m]
    [shinsetsu.schema :as s]
    [malli.error :as me]
    [buddy.hashers :as hashers]))

;; TODO: SPEC THESE SUCKERS!

(defn trim-tag [tag] (-> tag (dissoc :tag/user-id)))

(defmutation create-tag
  [{{user-id :user/id} :request} tag]
  {::pc/params #{:tag/name :tag/password}
   ::pc/output [:tag/id :tag/name :tag/colour :tag/created :tag/updated]}
  (let [tag (merge {:tag/user-id user-id} tag)]
    (if-let [err (m/explain s/tag-spec tag)]
      (throw (ex-info "Invalid tag" {:error-type :invalid-input :error-data (me/humanize err)}))
      (do
        (log/info "User" user-id "is attempting to create a new tag")
        (let [tag (-> tag db/create-tag trim-tag)]
          (log/info "User" user-id "created tag" (:tag/id tag) "successfully")
          tag)))))

(defmutation patch-tag
  [{{user-id :user/id} :request} {:tag/keys [id] :as patch-data}]
  {::pc/params #{:tag/name :tag/password}
   ::pc/output [:tag/id :tag/name :tag/colour :tag/created :tag/updated]}
  (let [patch-data (merge patch-data {:tag/user-id user-id})]
    (if-let [err (m/explain s/tag-update-spec patch-data)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
      (do
        (log/info "User" user-id "is attempting to update tag" id "info")
        (let [patched-tag (-> patch-data db/patch-tag trim-tag)]
          (log/info "User" user-id "patched tag" id "successfully")
          patched-tag)))))

(defmutation delete-tag
  [{{user-id :user/id} :request} {:tag/keys [id] :as input}]
  {::pc/params #{:tag/id}
   ::pc/output [:tag/id :tag/name :tag/colour :tag/created :tag/updated]}
  (let [input (merge input {:tag/user-id user-id})]
    (if-let [err (m/explain s/tag-delete-spec input)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
      (do
        (log/info "User with ID" user-id "is attempting to delete tag" id "info")
        (let [deleted-tag (-> input db/delete-tag trim-tag)]
          (log/info "User with ID" user-id "deleted tag" id "successfully")
          deleted-tag)))))
