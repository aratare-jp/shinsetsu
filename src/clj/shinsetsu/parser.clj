(ns shinsetsu.parser
  (:require
    [shinsetsu.config :as config]
    [com.fulcrologic.fulcro.server.api-middleware :refer [handle-api-request]]
    [mount.core :refer [defstate]]
    [shinsetsu.resolvers :as res]
    [shinsetsu.mutations :as mut]
    [com.wsscode.pathom.core :as p]
    [com.wsscode.pathom.connect :as pc]
    [taoensso.timbre :as log]
    [buddy.sign.jwt :as jwt]))

(def public-resolvers [res/public-resolvers mut/public-mutations])
(def protected-resolvers [res/protected-resolvers mut/protected-mutations])

(defn create-parser
  [resolvers]
  (p/parser {::p/env     {::p/reader                 [p/map-reader pc/reader2 pc/ident-reader pc/index-reader]
                          ::pc/mutation-join-globals [:tempids]
                          ::pc/process-error
                          (fn [_ err]
                            (.printStackTrace err)
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
    (log/info "Handling query.")
    (let [params (or (:transit-params req) (:body-params req))]
      (handle-api-request params (partial parser {:request req})))))

(def public-parser-handler (create-parser-handler public-parser))
(def protected-parser-handler (create-parser-handler protected-parser))

(comment
  (user/start)
  (user/restart))
