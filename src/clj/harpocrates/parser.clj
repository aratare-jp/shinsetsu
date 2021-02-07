(ns harpocrates.parser
  (:require
    [com.wsscode.pathom.core :as p]
    [com.wsscode.pathom.connect :as pc]
    [taoensso.timbre :as log]
    [harpocrates.resolvers]
    [harpocrates.mutations]
    [puget.printer :refer [pprint]]))

(def resolvers [harpocrates.resolvers/resolvers
                harpocrates.mutations/mutations])

(defn process-error
  "Overriding the default Pathom error handler so we can get the attached data on the client side."
  [env err]
  (.getData err))

(def pathom-parser
  (p/parser {::p/env     {::p/reader                 [p/map-reader
                                                      pc/reader2
                                                      pc/ident-reader
                                                      pc/index-reader]
                          ::p/process-error          process-error
                          ::pc/mutation-join-globals [:tempids]}
             ::p/mutate  pc/mutate
             ::p/plugins [(pc/connect-plugin {::pc/register resolvers})
                          p/error-handler-plugin]}))

(defn api-parser [query]
  (log/info "Process" query)
  (pathom-parser {} query))