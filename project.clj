(defproject harpocrates "0.1.0-SNAPSHOT"
  :description "Bookmark enhancer"
  :url "http://github.com/aratare-tech/harpocrates"
  :dependencies [[org.clojure/clojure "1.10.1"]]
  :profiles
  {
   ;; ------------------------------------------------------------------------------------------------------------------
   ;;
   ;; SHARED
   ;;
   ;; ------------------------------------------------------------------------------------------------------------------

   :shared      {:source-paths ["src/cljc"]
                 :dependencies [[com.fulcrologic/fulcro "3.4.12"]
                                [com.fulcrologic/fulcro-rad "1.0.8"]
                                [com.wsscode/pathom "2.3.1"]
                                [org.clojure/core.async "1.3.610"]
                                [medley "1.3.0"]
                                [prismatic/schema "1.1.12"]
                                [prismatic/schema-generators "0.1.3"]]}
   :shared-dev  [:shared
                 {:source-paths ["env/dev/cljc"]}]
   :shared-test [:shared-dev
                 {:source-paths ["env/test/cljc"]}]
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
                  :dependencies   [[org.clojure/tools.cli "1.0.194"]
                                   [cprop "0.1.17"]
                                   [http-kit "2.3.0"]
                                   [buddy "2.0.0"]
                                   [nrepl "0.8.3"]
                                   [ring "1.8.2"]
                                   [metosin/muuntaja "0.6.7"]
                                   [ring/ring-defaults "0.3.2"]
                                   [seancorfield/next.jdbc "1.1.646"]
                                   [honeysql "1.0.444"]
                                   [mount "0.1.16"]
                                   [metosin/reitit "0.5.11"]
                                   [bk/ring-gzip "0.3.0"]
                                   [migratus "1.3.5"]
                                   [valip "0.2.0"]
                                   [org.postgresql/postgresql "42.2.19.jre7"]
                                   [com.zaxxer/HikariCP "4.0.3"]
                                   [com.fzakaria/slf4j-timbre "0.3.21"]]
                  :target-path    "target/%s/"
                  :main           ^:skip-aot harpocrates.core
                  :clean-targets  ^{:protect false} [:target-path]
                  :repl-options   {:init-ns user
                                   :timeout 120000}}]
   :server-dev  [:shared-dev
                 :server
                 {:jvm-opts     ["-Dconf=dev-config.edn"]
                  :source-paths ["env/dev/clj"]
                  :dependencies [[org.clojure/tools.namespace "1.1.0"]
                                 [ring/ring-mock "0.4.0"]
                                 [mvxcvi/puget "1.3.1"]]}]
   :server-test [:shared-test
                 :server-dev
                 {:jvm-opts     ["-Dconf=test-config.edn"]
                  :source-paths ["env/test/clj"]
                  :test-paths   ["test/clj"]
                  :dependencies [[pjstadig/humane-test-output "0.10.0"]]
                  :injections   [(require 'pjstadig.humane-test-output)
                                 (pjstadig.humane-test-output/activate!)]}]
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
                 :dependencies [[org.clojure/clojurescript "1.10.773"]
                                [thheller/shadow-cljs "2.11.11"]
                                [venantius/accountant "0.2.5"]
                                [pez/clerk "1.0.0"]
                                [cljs-http "0.1.46"]
                                [kibu/pushy "0.3.8"]]}
   :client-dev  [:shared-dev
                 :client
                 {:source-paths ["env/dev/cljs"]
                  :dependencies [[binaryage/devtools "1.0.2"]]}]
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
                 :uberjar-name "harpocrates.jar"}
   :prod        [:client-prod
                 :server-prod
                 :uberjar]})