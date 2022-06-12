(ns build
  (:require [clojure.tools.build.api :as b]
            [org.corfield.build :as bb]))

(def lib 'com.github.aratare-jp/shinsetsu)
(def version "0.1.0")
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-name (str "target/shinsetsu-standalone.jar"))

(defn clean
  [_]
  (bb/clean {:target "target"}))

(defn uberjar
  [_]
  (bb/uber {:uber-file jar-name
            :version   version
            :lib       lib
            :main      'shinsetsu.server}))
