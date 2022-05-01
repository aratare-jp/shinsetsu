(ns shinsetsu.parser
  (:require
    [com.fulcrologic.fulcro.server.api-middleware :refer [handle-api-request]]
    [com.wsscode.pathom.connect :as pc]
    [com.wsscode.pathom.core :as p]
    [edn-query-language.core :as eql]
    [mount.core :as m]
    [shinsetsu.mutations.bookmark :as bookmark-mutations]
    [shinsetsu.mutations.bookmark-tag :as bookmark-tag-mutations]
    [shinsetsu.mutations.tab :as tab-mutations]
    [shinsetsu.mutations.tag :as tag-mutations]
    [shinsetsu.mutations.user :as user-mutations]
    [shinsetsu.resolvers.bookmark :as bookmark-resolver]
    [shinsetsu.resolvers.bookmark-tag :as bookmark-tag-resolver]
    [shinsetsu.resolvers.tab :as tab-resolver]
    [shinsetsu.resolvers.tag :as tag-resolver]
    [taoensso.timbre :as log])
  (:import [clojure.lang ExceptionInfo]))

(def public-resolvers
  [user-mutations/login
   user-mutations/register])

(def protected-resolvers
  [user-mutations/patch-user
   tab-resolver/tabs-resolver
   tab-mutations/create-tab
   tab-mutations/patch-tab
   tab-mutations/delete-tab
   bookmark-resolver/bookmark-resolver
   bookmark-resolver/bookmarks-resolver
   bookmark-mutations/create-bookmark
   bookmark-mutations/patch-bookmark
   bookmark-mutations/delete-bookmark
   bookmark-tag-resolver/bookmark-tag-resolver-by-bookmark
   bookmark-tag-mutations/create-bookmark-tag
   bookmark-tag-mutations/delete-bookmark-tag
   tag-resolver/tag-resolver
   tag-resolver/tags-resolver
   tag-mutations/create-tag
   tag-mutations/patch-tag
   tag-mutations/delete-tag])

(defn process-error
  [_ err]
  (log/error "An exception has occurred:" err)
  (log/error err)
  (if (instance? ExceptionInfo err)
    (let [data    (ex-data err)
          message (ex-message err)]
      (merge {:error true :error-message message} data))
    {:error true :error-message "Internal Server Error" :error-type :internal-server-error}))

;; Code copied from Fulcro RAD
(def query-params-to-env-plugin
  "Adds top-level load params to env, so nested parsing layers can see them."
  {::p/wrap-parser
   (fn [parser]
     (fn [env tx]
       (let [children     (-> tx eql/query->ast :children)
             query-params (reduce
                            (fn [qps {:keys [type params] :as x}]
                              (cond-> qps (and (not= :call type) (seq params)) (merge params)))
                            {}
                            children)
             env          (assoc env :query-params query-params)]
         (parser env tx))))})

(defn- raise-error
  [input]
  (if-let [errors (::p/errors input)]
    (reduce
      (fn [acc [k v]]
        (if (coll? k)
          (assoc acc (first k) v)
          (assoc acc k v)))
      {}
      errors)
    input))

(defn create-parser
  [resolvers]
  (p/parser {::p/env     {::p/reader                 [pc/open-ident-reader
                                                      p/map-reader
                                                      pc/reader2
                                                      pc/index-reader]
                          ::p/process-error          process-error
                          ::pc/mutation-join-globals [:tempids]}
             ::p/mutate  pc/mutate
             ::p/plugins [query-params-to-env-plugin
                          (pc/connect-plugin {::pc/register resolvers})
                          p/error-handler-plugin
                          p/trace-plugin
                          (p/post-process-parser-plugin (comp raise-error p/elide-not-found))]}))

(def public-parser (create-parser public-resolvers))
(def protected-parser (create-parser protected-resolvers))

(defn create-parser-handler
  [parser]
  (fn [req]
    (let [params (or (:transit-params req) (:body-params req))]
      (handle-api-request params (partial parser {:request req})))))

(m/defstate public-parser-handler
  :start
  (create-parser-handler public-parser))

(m/defstate protected-parser-handler
  :start
  (create-parser-handler protected-parser))

(comment
  (let [query '[({[:tab/id "a7cd12da-1596-4158-9e1d-63a26d24efd4"] [:tab/bookmarks]} {:tab/password "boo"})]]
    (protected-parser {} query))

  (user/start)
  (user/restart))
