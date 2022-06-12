(ns build
  (:require [clojure.tools.build.api :as b]
            [org.corfield.build :as bb]))

(def lib 'com.github.aratare-jp/shinsetsu)
(def version (format "0.1.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-name (str "target/shinsetsu-" version "-standalone.jar"))

(defn clean
  [_]
  (bb/clean {:target "target"}))

(defn jar
  [_]
  (bb/jar {:lib lib :version version}))

(defn uberjar
  [_]
  (bb/uber {:uber-file jar-name
            :version   version
            :lib       lib
            :main      'shinsetsu.server}))
