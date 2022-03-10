(ns shinsetsu.parser
  (:require
    [com.fulcrologic.fulcro.server.api-middleware :refer [handle-api-request]]
    [mount.core :refer [defstate]]
    [shinsetsu.resolvers :as res]
    [shinsetsu.mutations :as mut]
    [com.wsscode.pathom.core :as p]
    [com.wsscode.pathom.connect :as pc]
    [taoensso.timbre :as log]))

(def resolvers [res/resolvers mut/mutations])

(def pathom-parser
  (p/parser {::p/env     {::p/reader                 [p/map-reader
                                                      pc/reader2
                                                      pc/ident-reader
                                                      pc/index-reader]
                          ::pc/mutation-join-globals [:tempids]
                          ::pc/process-error
                          (fn [_ err]
                            (.printStackTrace err)
                            (p/error-str err))}
             ::p/mutate  pc/mutate
             ::p/plugins [(pc/connect-plugin {::pc/register resolvers})
                          p/error-handler-plugin
                          (p/post-process-parser-plugin p/elide-not-found)]}))

(defn pathom-handler
  [_]
  (fn [req]
    (let [params (or (:transit-params req) (:body-params req))]
      (handle-api-request params (partial pathom-parser {:request req})))))

(comment
  (user/restart))
