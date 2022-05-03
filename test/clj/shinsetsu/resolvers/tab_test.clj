(ns shinsetsu.resolvers.tab-test
  (:require
    [buddy.hashers :as hashers]
    [clojure.test :refer :all]
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.db.tab :as tab-db]
    [shinsetsu.db.user :as user-db]
    [shinsetsu.parser :refer [protected-parser]]
    [shinsetsu.test-utility :refer [db-cleanup db-setup]]))

(def user-id (atom nil))

(defn user-setup
  [f]
  (let [username "john"
        password "smith"
        user     (user-db/create-user {:user/username username :user/password password})]
    (reset! user-id (:user/id user))
    (f)))

(use-fixtures :once db-setup)
(use-fixtures :each db-cleanup user-setup)

(def tab-join [:tab/id :tab/name :tab/is-protected? :tab/password :tab/created :tab/updated])

(defn create-tab
  [{:tab/keys [password] :as t}]
  (-> (if password (update t :tab/password hashers/derive) t)
      (tab-db/create-tab)
      (assoc :tab/is-protected? (not (nil? password)))
      (select-keys [:tab/id :tab/name :tab/is-protected?])))

(defexpect normal-fetch-tabs
  (let [tab1     (create-tab {:tab/name "foo" :tab/password "bar" :tab/user-id @user-id})
        tab2     (create-tab {:tab/name "foo1" :tab/password "bar2" :tab/user-id @user-id})
        tab3     (create-tab {:tab/name "foo1" :tab/user-id @user-id})
        expected {:user/tabs [tab1 tab2 tab3]}
        actual   (protected-parser {:request {:user/id @user-id}} [{:user/tabs tab-join}])]
    (expect expected actual)))

(defexpect normal-fetch-empty-tabs
  (let [expected {:user/tabs []}
        actual   (protected-parser {:request {:user/id @user-id}} [{:user/tabs tab-join}])]
    (expect expected actual)))

(comment
  (require '[kaocha.repl :as k])
  (require '[shinsetsu.parser :refer [protected-parser]])
  (k/run 'shinsetsu.resolvers.tab-test)
  (k/run #'shinsetsu.resolvers.tab-test/normal-fetch-tabs))
