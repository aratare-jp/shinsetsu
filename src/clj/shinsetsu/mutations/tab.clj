(ns shinsetsu.mutations.tab
  (:require
    [shinsetsu.db.tab :as db]
    [com.wsscode.pathom.connect :as pc :refer [defmutation]]
    [taoensso.timbre :as log]
    [malli.core :as m]
    [shinsetsu.schema :as s]
    [malli.error :as me]
    [buddy.hashers :as hashers]))

;; TODO: SPEC THESE SUCKERS!

(defn trim-tab [tab] (-> tab (dissoc :tab/password) (dissoc :tab/user-id)))

(defn hash-password
  [tab]
  (if-let [password (:tab/password tab)]
    (merge tab {:tab/password (hashers/derive password)})
    tab))

(defmutation create-tab
  [{{user-id :user/id} :request} tab]
  {::pc/params #{:tab/name :tab/password}
   ::pc/output [:tab/id :tab/name :tab/is-protected? :tab/created :tab/updated]}
  (let [tab (merge {:tab/user-id user-id} tab)]
    (if-let [err (m/explain s/tab-spec tab)]
      (throw (ex-info "Invalid tab" {:error-type :invalid-input :error-data (me/humanize err)}))
      (do
        (log/info "User" user-id "is attempting to create a new tab")
        (let [tab (-> tab hash-password db/create-tab trim-tab)]
          (log/info "User" user-id "created tab" (:tab/id tab) "successfully")
          tab)))))

(defmutation patch-tab
  [{{user-id :user/id} :request} {:tab/keys [id] :as patch-data}]
  {::pc/params #{:tab/name :tab/password}
   ::pc/output [:tab/id :tab/name :tab/is-protected? :tab/created :tab/updated]}
  (if (empty? patch-data)
    {:user/id user-id}
    (let [patch-data (merge patch-data {:tab/user-id user-id})]
      (if-let [err (m/explain s/tab-update-spec patch-data)]
        (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
        (do
          (log/info "User" user-id "is attempting to update tab" id "info")
          (let [patched-tab (-> patch-data hash-password db/patch-tab trim-tab)]
            (log/info "User" user-id "patched tab" id "successfully")
            patched-tab))))))

(defmutation delete-tab
  [{{user-id :user/id} :request} {:tab/keys [id] :as input}]
  {::pc/params #{:tab/id}
   ::pc/output [:tab/id :tab/name :tab/is-protected? :tab/created :tab/updated]}
  (let [input (merge input {:tab/user-id user-id})]
    (if-let [err (m/explain s/tab-delete-spec input)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
      (do
        (log/info "User with ID" user-id "is attempting to delete tab" id "info")
        (let [deleted-tab (-> input db/delete-tab trim-tab)]
          (log/info "User with ID" user-id "deleted tab" id "successfully")
          deleted-tab)))))
