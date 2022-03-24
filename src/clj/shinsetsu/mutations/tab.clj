(ns shinsetsu.mutations.tab
  (:require
    [shinsetsu.db.tab :as db]
    [com.wsscode.pathom.connect :as pc :refer [defmutation]]
    [taoensso.timbre :as log]
    [malli.core :as m]
    [shinsetsu.schema :as s]
    [malli.error :as me]
    [buddy.hashers :as hashers]))

(def tab-output [:tab/id :tab/name :tab/is-protected? :tab/created :tab/updated])

(defn trim-tab
  [t]
  (if t
    (-> t
        (assoc :tab/is-protected? (boolean (:tab/password t)))
        (select-keys tab-output))))

(defn hash-password [t] (if (:tab/password t) (update t :tab/password hashers/derive) t))

(defmutation create-tab
  [{{user-id :user/id} :request} tab]
  {::pc/params #{:tab/name :tab/password}
   ::pc/output tab-output}
  (let [tab (merge {:tab/user-id user-id} tab)]
    (if-let [err (m/explain s/tab-create-spec tab)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
      (do
        (log/info "User" user-id "is attempting to create a new tab")
        (let [tab (-> tab hash-password db/create-tab trim-tab)]
          (log/info "User" user-id "created tab" (:tab/id tab) "successfully")
          tab)))))

(defmutation fetch-tab
  [{{user-id :user/id} :request} {:tab/keys [id password] :as input}]
  {::pc/params #{:tab/id :tab/password}
   ::pc/output [:tab/id :tab/name :tab/is-protected? :tab/created :tab/updated]}
  (log/info "User" user-id "requested tab" id)
  (let [input (merge {:tab/user-id user-id} input)]
    (if-let [err (m/explain s/tab-fetch-spec input)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
      (let [tab (db/fetch-tab input)]
        (if-let [pwd (:tab/password tab)]
          (if (hashers/check password pwd)
            (trim-tab tab)
            (do
              (log/warn "User" user-id "attempted to fetch tab" id "with wrong password")
              (throw (ex-info "Invalid input" {:error-type :wrong-password}))))
          (trim-tab tab))))))

(defmutation patch-tab
  [{{user-id :user/id} :request} {:tab/keys [id] :as patch-data}]
  {::pc/params #{:tab/name :tab/password}
   ::pc/output tab-output}
  (let [patch-data (merge patch-data {:tab/user-id user-id})]
    (if-let [err (m/explain s/tab-patch-spec patch-data)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
      (do
        (log/info "User" user-id "is attempting to update tab" id "info")
        (let [patched-tab (-> patch-data hash-password db/patch-tab trim-tab)]
          (log/info "User" user-id "patched tab" id "successfully")
          patched-tab)))))

(defmutation delete-tab
  [{{user-id :user/id} :request} {:tab/keys [id] :as input}]
  {::pc/params #{:tab/id}
   ::pc/output tab-output}
  (let [input (merge input {:tab/user-id user-id})]
    (if-let [err (m/explain s/tab-delete-spec input)]
      (throw (ex-info "Invalid input" {:error-type :invalid-input :error-data (me/humanize err)}))
      (do
        (log/info "User with ID" user-id "is attempting to delete tab" id "info")
        (let [deleted-tab (-> input db/delete-tab trim-tab)]
          (log/info "User with ID" user-id "deleted tab" id "successfully")
          deleted-tab)))))
