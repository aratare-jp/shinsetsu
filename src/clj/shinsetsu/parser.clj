(ns shinsetsu.parser
  (:require
    [com.wsscode.pathom.core :as p]
    [com.wsscode.pathom.connect :as pc]
    [taoensso.timbre :as log]
    [shinsetsu.resolvers]
    [shinsetsu.mutations]
    [mount.core :refer [defstate]]
    [puget.printer :refer [pprint]]))

(def resolvers [shinsetsu.resolvers/resolvers
                shinsetsu.mutations/mutations])

(defn process-error
  "Overriding the default Pathom error handler so we can get the attached data on the client side."
  [env err]
  (tap> env)
  (tap> err)
  (log/error "Error found" err)
  (.printStackTrace err)
  err)

(defonce parser (atom nil))

(defstate pathom-parser
  :start
  (reset! parser (p/parser {::p/env     {::p/reader                 [p/map-reader
                                                                     pc/reader2
                                                                     pc/open-ident-reader]
                                         ::p/process-error          process-error
                                         ::pc/mutation-join-globals [:tempids]}
                            ::p/mutate  pc/mutate
                            ::p/plugins [(pc/connect-plugin {::pc/register resolvers})
                                         (p/env-plugin {})
                                         (p/post-process-parser-plugin p/elide-not-found)
                                         p/error-handler-plugin]}))
  :stop
  (reset! parser nil))