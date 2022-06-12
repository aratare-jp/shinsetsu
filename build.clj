(ns build
  (:require [clojure.tools.build.api :as b]
            [org.corfield.build :as bb]))

(def lib 'com.github.aratare-jp/shinsetsu)
(def version (format "0.1.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-name (str "target/shinsetsu-" version "-standalone.jar"))

(defn pom
  [_]
  (b/write-pom {:target   "."
                :lib      lib
                :version  version
                :basis    basis
                :src-dirs ["src/clj" "src/cljc"]}))

(defn uber
  [_]
  (bb/uber {:uber-file jar-name
            :version   version
            :lib       lib
            :main      'shinsetsu.server}))
