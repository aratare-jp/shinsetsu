(defproject harpocrates "0.1.0-SNAPSHOT"

  :description "Bookmark enhancer"
  :url "http://github.com/aratare-tech/harpocrates"

  :dependencies [[cprop "0.1.16"]
                 [metosin/reitit "0.4.2"]]
  ;:dependencies [[cheshire "5.10.0"]
  ;               [clojure.java-time "0.3.2"]
  ;               [com.cognitect/transit-clj "1.0.324"]
  ;               [com.walmartlabs/lacinia "0.32.0"]
  ;               [conman "0.8.6"]
  ;
  ;               [expound "0.8.4"]
  ;               [funcool/struct "1.4.0"]
  ;               [luminus-http-kit "0.1.6"]
  ;               [luminus-migrations "0.6.7"]
  ;               [luminus-transit "0.1.2"]
  ;               [metosin/muuntaja "0.6.6"]
  ;
  ;               [metosin/ring-http-response "0.9.1"]
  ;               [nrepl "0.7.0"]
  ;               [org.clojure/clojure "1.10.1"]
  ;               [org.clojure/tools.namespace "0.2.11"]
  ;               [org.clojure/data.json "0.2.6"]
  ;               [org.clojure/tools.cli "1.0.194"]
  ;               [org.clojure/tools.logging "1.0.0"]
  ;               [ch.qos.logback/logback-classic "1.2.3"]
  ;               [org.postgresql/postgresql "42.2.11"]
  ;               [org.webjars.npm/bulma "0.8.0"]
  ;               [org.webjars.npm/material-icons "0.3.1"]
  ;               [org.webjars/webjars-locator "0.39"]
  ;               [org.webjars/webjars-locator-jboss-vfs "0.1.0"]
  ;               [ring-webjars "0.2.0"]
  ;               [ring/ring-core "1.8.0"]
  ;               [ring/ring-defaults "0.3.2"]
  ;
  ;               [http-kit "2.3.0"]
  ;               [buddy/buddy-hashers "1.4.0"]
  ;               [buddy/buddy-auth "2.2.0"]
  ;               [ring-logger "1.0.1"]]

  :min-lein-version "2.0.0"

  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :test-paths ["test/clj" "test/cljs"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :main ^:skip-aot harpocrates.core

  :clean-targets ^{:protect false} [:target-path]

  :aliases {"fig"        ["trampoline" "run" "-m" "figwheel.main"]
            "fig-config" ["fig" "--" "-pc" "-b" "dev"]
            "fig-dev"    ["fig" "--" "-b" "dev" "-r"]
            "fig-prod"   ["with-profile" "prod" "fig" "--" "--optimizations"
                          "advanced" "--build-once" "prod"]}

  :profiles
  {:prod          [:uberjar]
   :uberjar       {:omit-source    true
                   :prep-tasks     ["compile"]
                   :aot            :all
                   :uberjar-name   "harpocrates.jar"
                   :source-paths   ["env/prod/clj" "env/prod/cljs"]
                   :resource-paths ["env/prod/resources"]}
   :test          [:dev :project/test]
   :cljs          {:dependencies   [[org.clojure/clojurescript "1.10.773"]
                                    [binaryage/devtools "1.0.0"]
                                    [day8.re-frame/re-frame-10x "0.6.4"]
                                    [thheller/shadow-cljs "2.10.3"]
                                    [re-frame "0.12.0"]
                                    [day8.re-frame/http-fx "v0.2.0"]
                                    [reagent "0.10.0"]
                                    [reagent-utils "0.3.3"]
                                    [mount "0.1.16"]
                                    [venantius/accountant "0.2.5"]
                                    [cljs-ajax "0.8.0"]
                                    [pez/clerk "1.0.0"]]
                   :source-paths   ["env/dev/cljs" "target"]
                   :resource-paths ["env/dev/resources"]}
   :dev           {:jvm-opts       ["-Dconf=dev-config.edn"]
                   :dependencies   [[cider/piggieback "0.4.2"]
                                    [doo "0.1.11"]
                                    [pjstadig/humane-test-output "0.10.0"]
                                    [prone "2020-01-17"]
                                    [re-frisk "0.5.5"]
                                    [ring/ring-devel "1.8.0"]
                                    [ring/ring-mock "0.4.0"]]
                   :plugins        [[com.jakemccrary/lein-test-refresh "0.24.1"]
                                    [jonase/eastwood "0.3.5"]
                                    [lein-doo "0.1.11"]]
                   :doo            {:build "test"
                                    :alias {:default [:chrome]}}
                   :source-paths   ["env/dev/clj" "env/dev/cljs" "target"]
                   :resource-paths ["env/dev/resources"]
                   :repl-options   {:init-ns user
                                    :timeout 120000}
                   :injections     [(require 'pjstadig.humane-test-output)
                                    (pjstadig.humane-test-output/activate!)]}
   :project/test  {:jvm-opts       ["-Dconf=test-config.edn"]
                   :resource-paths ["env/test/resources"]}
   :profiles/test {}})
