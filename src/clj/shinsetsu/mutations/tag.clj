(ns shinsetsu.mutations.tag
  (:require
    [shinsetsu.db.tag :as db]
    [com.wsscode.pathom.connect :as pc :refer [defmutation]]
    [taoensso.timbre :as log]
    [malli.core :as m]
    [shinsetsu.schema :as s]
    [malli.error :as me]))

(def tag-output [:tag/id :tag/name :tag/colour :tag/created :tag/updated])
(defn trim-tag [tag] (-> tag (dissoc :tag/user-id)))

(defmutation create-tag
  [{{user-id :user/id} :request} tag]
  {::pc/params #{:tag/name :tag/colour}
   ::pc/output tag-output}
  (let [tag (merge {:tag/user-id user-id} tag)]
    (if-let [err (m/explain s/tag-create-spec tag)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
      (do
        (log/info "User" user-id "is attempting to create a new tag")
        (let [tempid  (:tag/id tag)
              tag     (-> tag (dissoc :tag/id) db/create-tag trim-tag)
              real-id (:tag/id tag)]
          (log/info "User" user-id "created tag" (:tag/id tag) "successfully")
          (if tempid
            (merge tag {:tempids {tempid real-id}})
            tag))))))

(defmutation patch-tag
  [{{user-id :user/id} :request} {:tag/keys [id] :as patch-data}]
  {::pc/params #{:tag/name :tag/colour}
   ::pc/output tag-output}
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
   ::pc/output tag-output}
  (let [input (merge input {:tag/user-id user-id})]
    (if-let [err (m/explain s/tag-delete-spec input)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
      (do
        (log/info "User with ID" user-id "is attempting to delete tag" id "info")
        (let [deleted-tag (-> input db/delete-tag trim-tag)]
          (log/info "User with ID" user-id "deleted tag" id "successfully")
          deleted-tag)))))

(defmutation fetch-tags
  [{{user-id :user/id} :request} input]
  {::pc/params #{:tag/name :tag/name-pos}
   ::pc/output [{:user/tags tag-output}]}
  (let [input (merge input {:tag/user-id user-id})]
    (if-let [err (m/explain s/tag-multi-fetch-spec input)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)})))
    (log/info "User" user-id "requested all tags")
    (let [tags (db/fetch-tags input)]
      (log/info "All tags fetched from user" user-id)
      {:user/tags (mapv trim-tag tags)})))

(comment
  (str "boo" nil "boo"))
