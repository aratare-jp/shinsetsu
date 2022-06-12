(ns build
  (:require [clojure.tools.build.api :as b]
            [org.corfield.build :as bb]))

(def lib 'com.github.aratare-jp/shinsetsu)
;; if you want a version of MAJOR.MINOR.COMMITS:
(def version (format "1.0.%s" (b/git-count-revs nil)))
(defn uber
  [_]
  (bb/uber {:uber-file "target/shinsetsu-standalone.jar"
            :version   version
            :lib       lib
            :main      'shinsetsu.server}))
