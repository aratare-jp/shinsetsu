(ns shinsetsu.mutations
  (:require
    [com.wsscode.pathom.connect :as pc]
    [taoensso.timbre :as log]))

(pc/defmutation login
  [env {:keys [username password]}]
  {::pc/sym `login}
  (log/info "User with username" username "is attempting to login...")
  {:token "hello-world!"})

(def mutations [login])
