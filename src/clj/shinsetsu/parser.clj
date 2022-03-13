(ns shinsetsu.parser
  (:require
    [com.fulcrologic.fulcro.server.api-middleware :refer [handle-api-request]]
    [mount.core :refer [defstate]]
    [shinsetsu.resolvers.tab :as tab-resolver]
    [shinsetsu.resolvers.bookmark :as bookmark-resolver]
    [shinsetsu.mutations.auth :as auth-mutations]
    [shinsetsu.mutations.tab :as tab-mutations]
    [shinsetsu.mutations.bookmark :as bookmark-mutations]
    [com.wsscode.pathom.core :as p]
    [com.wsscode.pathom.connect :as pc]
    [taoensso.timbre :as log]))

(def public-resolvers
  [auth-mutations/login
   auth-mutations/register])

(def protected-resolvers
  [tab-resolver/tabs-resolver
   tab-resolver/tab-resolver
   tab-resolver/tab-bookmarks-resolver
   bookmark-resolver/bookmark-resolver
   tab-mutations/create-tab])

(defn create-parser
  [resolvers]
  (p/parser {::p/env     {::p/reader                 [p/map-reader pc/reader2 pc/ident-reader pc/index-reader]
                          ::pc/mutation-join-globals [:tempids]
                          ::pc/process-error         (fn [env err]
                                                       (log/error err)
                                                       (p/error-str err))}
             ::p/mutate  pc/mutate
             ::p/plugins [(pc/connect-plugin {::pc/register resolvers})
                          p/error-handler-plugin
                          (p/post-process-parser-plugin p/elide-not-found)]}))

(def public-parser (create-parser public-resolvers))
(def protected-parser (create-parser protected-resolvers))

(defn create-parser-handler
  [parser]
  (fn [req]
    (let [params (or (:transit-params req) (:body-params req))]
      (handle-api-request params (partial parser {:request req})))))

(def public-parser-handler (create-parser-handler public-parser))
(def protected-parser-handler (create-parser-handler protected-parser))

(comment
  (user/start)
  (user/restart))
