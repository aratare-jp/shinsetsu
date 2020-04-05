(ns harpocrates.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [harpocrates.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[harpocrates started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[harpocrates has shut down successfully]=-"))
   :middleware wrap-dev})
