(defproject shinsetsu "0.1.0-SNAPSHOT"
  :description "Bookmark enhancer"
  :url "http://github.com/aratare-tech/shinsetsu"
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [com.github.liquidz/antq "RELEASE"]]
  :aliases {"outdated" ["run" "-m" "antq.core"]}
  :profiles
  {
   ;; ------------------------------------------------------------------------------------------------------------------
   ;;
   ;; SHARED
   ;;
   ;; ------------------------------------------------------------------------------------------------------------------

   :shared      {:source-paths ["src/cljc"]
                 :dependencies [[com.fulcrologic/fulcro "3.4.21"]
                                [com.fulcrologic/fulcro-rad "1.0.9"]
                                [com.wsscode/pathom "2.3.1"]
                                [org.clojure/core.async "1.3.618"]
                                [medley "1.3.0"]
                                [prismatic/schema "1.1.12"]
                                [prismatic/schema-generators "0.1.3"]
                                [camel-snake-kebab "0.4.2"]]}
   :shared-dev  [:shared
                 {:source-paths ["env/dev/cljc"]}]
   :shared-test [:shared-dev
                 {:source-paths ["env/test/cljc"]
                  :dependencies [[eftest "0.5.9"]
                                 [tortue/spy "2.4.0"]
                                 [expectations/clojure-test "1.2.1"]
                                 [clj-test-containers "0.4.0"]]
                  :plugins      [[lein-eftest "0.5.9"]
                                 [lein-cloverage "1.2.2"]]}]
   :shared-prod [:shared
                 {:source-paths ["env/prod/cljc"]}]

   ;; ------------------------------------------------------------------------------------------------------------------
   ;;
   ;; SERVER
   ;;
   ;; ------------------------------------------------------------------------------------------------------------------

   :server      [:shared
                 {:source-paths   ["src/clj"]
                  :resource-paths ["resources"]
                  :dependencies   [[org.clojure/tools.cli "1.0.206"]
                                   [cprop "0.1.17"]
                                   [http-kit "2.5.3"]
                                   [buddy "2.0.0"]
                                   [nrepl "0.8.3"]
                                   [ring "1.9.3"]
                                   [metosin/muuntaja "0.6.8"]
                                   [ring/ring-defaults "0.3.2"]
                                   [seancorfield/next.jdbc "1.2.659"]
                                   [com.github.seancorfield/honeysql "2.0.0-rc2"]
                                   [mount "0.1.16"]
                                   [metosin/reitit "0.5.13"]
                                   [bk/ring-gzip "0.3.0"]
                                   [migratus "1.3.5"]
                                   [valip "0.2.0"]
                                   [org.postgresql/postgresql "42.2.20"]
                                   [com.zaxxer/HikariCP "4.0.3"]
                                   [com.fzakaria/slf4j-timbre "0.3.21"]
                                   [cheshire "5.10.0"]
                                   [com.cognitect/transit-clj "1.0.324"]]
                  :target-path    "target/%s/"
                  :main           ^:skip-aot shinsetsu.core
                  :clean-targets  ^{:protect false} [:target-path]}]
   :server-dev  [:shared-dev
                 :server
                 {:jvm-opts     ["-Dconf=dev-config.edn"]
                  :source-paths ["env/dev/clj"]
                  :dependencies [[org.clojure/tools.namespace "1.1.0"]
                                 [mvxcvi/puget "1.3.1"]]
                  :repl-options {:init-ns user
                                 :timeout 120000}}]
   :server-test [:shared-test
                 :server
                 {:jvm-opts     ["-Dconf=test-config.edn"]
                  :source-paths ["env/test/clj"]
                  :test-paths   ["test/clj"]
                  :dependencies [[ring/ring-mock "0.4.0"]
                                 [org.clojure/tools.namespace "1.1.0"]]
                  :repl-options {:init-ns user
                                 :timeout 120000}
                  :eftest       {:fail-fast?   true
                                 :multithread? :namespaces}
                  :runner       :eftest
                  :runner-opts  {:fail-fast?   true
                                 :multithread? :namespaces}}]
   :server-prod [:shared-prod
                 :server
                 {:jvm-opts     ["-Dconf=prod-config.edn"]
                  :source-paths ["env/prod/clj"]}]

   ;; ------------------------------------------------------------------------------------------------------------------
   ;;
   ;; CLIENT
   ;;
   ;; ------------------------------------------------------------------------------------------------------------------

   :client      {:source-paths ["src/cljs"]
                 :dependencies [[org.clojure/clojurescript "1.10.866"]
                                [thheller/shadow-cljs "2.14.0"]
                                [venantius/accountant "0.2.5"]
                                [pez/clerk "1.0.0"]
                                [cljs-http "0.1.46"]
                                [kibu/pushy "0.3.8"]]}
   :client-dev  [:shared-dev
                 :client
                 {:source-paths ["env/dev/cljs"]
                  :dependencies [[binaryage/devtools "1.0.3"]]}]
   :client-test [:shared-test
                 :client-dev
                 {:source-paths ["env/test/cljs"]
                  :test-paths   ["test/cljs"]}]
   :client-prod [:shared-prod
                 :client
                 {:source-paths ["env/prod/cljs"]}]
   :client-demo [:shared-prod
                 :client
                 {:source-paths ["env/demo/cljs"]}]

   ;; ------------------------------------------------------------------------------------------------------------------
   ;;
   ;; PROD
   ;;
   ;; ------------------------------------------------------------------------------------------------------------------

   :uberjar     {:omit-source  true
                 :prep-tasks   ["compile"]
                 :aot          :all
                 :uberjar-name "shinsetsu.jar"}
   :prod        [:client-prod
                 :server-prod
                 :uberjar]})