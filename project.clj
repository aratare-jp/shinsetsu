(defproject harpocrates "0.1.0-SNAPSHOT"
  :description "Bookmark enhancer"
  :url "http://github.com/aratare-tech/harpocrates"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.773"]
                 [mount "0.1.16"]
                 [cprop "0.1.17"]
                 [metosin/reitit "0.5.11"]
                 [thheller/shadow-cljs "2.11.11"]
                 [com.fulcrologic/fulcro "3.4.12"]
                 [seancorfield/next.jdbc "1.1.613"]
                 [honeysql "1.0.444"]
                 [venantius/accountant "0.2.5"]
                 [pez/clerk "1.0.0"]
                 [buddy "2.0.0"]
                 [http-kit "2.3.0"]
                 [nrepl "0.8.3"]
                 [tick "0.4.24-alpha"]]
  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :test-paths ["test/clj" "test/cljs" "test/cljc"]
  :resource-paths ["resources"]
  :profiles
  {:dev {:dependencies [[pjstadig/humane-test-output "0.10.0"]
                        [binaryage/devtools "1.0.2"]]
         :injections   [(require 'pjstadig.humane-test-output)
                        (pjstadig.humane-test-output/activate!)]}})