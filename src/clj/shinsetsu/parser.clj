(ns shinsetsu.parser
  (:require
    [com.fulcrologic.fulcro.server.api-middleware :refer [handle-api-request]]
    [mount.core :refer [defstate]]
    [shinsetsu.resolvers.tab :as tab-resolver]
    [shinsetsu.resolvers.bookmark :as bookmark-resolver]
    [shinsetsu.resolvers.tag :as tag-resolver]
    [shinsetsu.mutations.user :as user-mutations]
    [shinsetsu.mutations.tab :as tab-mutations]
    [shinsetsu.mutations.bookmark :as bookmark-mutations]
    [shinsetsu.mutations.tag :as tag-mutations]
    [com.wsscode.pathom.core :as p]
    [com.wsscode.pathom.connect :as pc]
    [taoensso.timbre :as log])
  (:import [clojure.lang ExceptionInfo]))

(def public-resolvers
  [user-mutations/login
   user-mutations/register])

(def protected-resolvers
  [user-mutations/patch-user
   tab-resolver/tabs-resolver
   tab-resolver/tab-resolver
   tab-mutations/create-tab
   tab-mutations/patch-tab
   tab-mutations/delete-tab
   bookmark-resolver/bookmarks-resolver
   bookmark-resolver/bookmark-resolver
   bookmark-mutations/create-bookmark
   bookmark-mutations/patch-bookmark
   bookmark-mutations/delete-bookmark
   bookmark-mutations/create-bookmark-tag
   bookmark-mutations/delete-bookmark-tag
   tag-resolver/tag-resolver
   tag-resolver/tags-resolver
   tag-resolver/bookmark-tag-resolver
   tag-mutations/create-tag
   tag-mutations/patch-tag
   tag-mutations/delete-tag])

(defn process-error
  [env err]
  (log/error err)
  (if (instance? ExceptionInfo err)
    (let [data    (ex-data err)
          message (ex-message err)]
      (merge {:error true :error-message message} data))
    {:error true :error-message "Internal Server Error" :error-type :internal-server-error}))

(defn create-parser
  [resolvers]
  (p/parser {::p/env     {::p/reader                 [p/map-reader pc/reader2 pc/ident-reader pc/index-reader]
                          ::p/process-error          process-error
                          ::pc/mutation-join-globals [:tempids]}
             ::p/mutate  pc/mutate
             ::p/plugins [(pc/connect-plugin {::pc/register resolvers})
                          p/error-handler-plugin
                          p/trace-plugin
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
