(ns build
    (:require [clojure.tools.build.api :as b]
      [org.corfield.build :as bb]))

(def lib 'rei/shinsetsu)
(def version (format "1.0.%s" (b/git-count-revs nil)))
