(defproject harpocrates "0.1.0-SNAPSHOT"

  :description "Bookmark enhancer"
  :url "http://github.com/aratare-tech/harpocrates"

  :dependencies [[mount "0.1.16"]
                 [cprop "0.1.16"]
                 [metosin/reitit "0.4.2"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.cli "1.0.194"]
                 [org.clojure/tools.logging "1.0.0"]
                 [org.clojure/clojurescript "1.10.773"]
                 [org.clojure/algo.generic "0.1.3"]
                 [com.fulcrologic/fulcro "3.2.6"]
                 [com.wsscode/pathom "2.2.15"]
                 [org.clojure/spec.alpha "0.2.187"]]

  :min-lein-version "2.0.0"

  :source-paths ["src/cljc"]

  :profiles
  {:prod          [:client-prod :server-prod :uberjar]
   :uberjar       {:omit-source    true
                   :prep-tasks     ["compile"]
                   :aot            :all
                   :uberjar-name   "harpocrates.jar"
                   :resource-paths ["env/prod/resources"]}

   :common-dev    {:dependencies [[pjstadig/humane-test-output "0.10.0"]]}
   :plugins       [[com.jakemccrary/lein-test-refresh "0.24.1"]
                   [jonase/eastwood "0.3.5"]]

   :client        {:dependencies   [[thheller/shadow-cljs "2.11.4"]
                                    [venantius/accountant "0.2.5"]
                                    [cljs-ajax "0.8.0"]
                                    [pez/clerk "1.0.0"]]
                   :source-paths   ["src/cljs"]
                   :test-paths     ["test/cljs"]
                   :resource-paths ["resources"]}

   :client-dev    [:common-dev
                   :client
                   {:dependencies   [[cider/piggieback "0.5.0"]
                                     [binaryage/devtools "1.0.0"]
                                     [fulcrologic/fulcro-inspect "2.2.5"]]
                    :source-paths   ["env/dev/cljs" "target"]
                    :resource-paths ["env/dev/resources"]}]

   :client-prod   [:client
                   {:source-paths   ["env/prod/cljs"]
                    :resource-paths ["env/prod/resources"]}]

   :server        {:dependencies   [[selmer "1.12.27"]
                                    [cheshire "5.10.0"]
                                    [clojure.java-time "0.3.2"]
                                    [com.cognitect/transit-clj "1.0.324"]
                                    [com.walmartlabs/lacinia "0.32.0"]
                                    [conman "0.8.6"]
                                    [expound "0.8.4"]
                                    [funcool/struct "1.4.0"]
                                    [luminus-http-kit "0.1.6"]
                                    [luminus-migrations "0.6.7"]
                                    [luminus-transit "0.1.2"]
                                    [metosin/muuntaja "0.6.6"]
                                    [metosin/ring-http-response "0.9.1"]
                                    [nrepl "0.7.0"]
                                    [ch.qos.logback/logback-classic "1.2.3"]
                                    [org.postgresql/postgresql "42.2.11"]
                                    [ring-webjars "0.2.0"]
                                    [ring/ring-core "1.8.0"]
                                    [ring/ring-defaults "0.3.2"]
                                    [ring-cors "0.1.13"]
                                    [http-kit "2.3.0"]
                                    [buddy/buddy-hashers "1.4.0"]
                                    [buddy/buddy-auth "2.2.0"]
                                    [ring-logger "1.0.1"]
                                    [toucan "1.15.1"]
                                    [orchestra "2020.09.18-1"]]
                   :source-paths   ["src/clj"]
                   :test-paths     ["test/clj"]
                   :resource-paths ["resources"]
                   :target-path    "target/%s/"
                   :main           ^:skip-aot harpocrates.core
                   :clean-targets  ^{:protect false} [:target-path]}

   :server-dev    [:common-dev
                   :server
                   {:dependencies   [[prone "2020-01-17"]
                                     [ring/ring-devel "1.8.0"]
                                     [ring/ring-mock "0.4.0"]]
                    :jvm-opts       ["-Dconf=dev-config.edn"]
                    :source-paths   ["env/dev/clj"]
                    :resource-paths ["env/dev/resources"]
                    :repl-options   {:init-ns user
                                     :timeout 120000}
                    :injections     [(require 'pjstadig.humane-test-output)
                                     (pjstadig.humane-test-output/activate!)]}]

   :server-prod   [:server
                   {:jvm-opts       ["-Dconf=prod-config.edn"]
                    :source-paths   ["env/prod/clj"]
                    :resource-paths ["env/prod/resources"]}]

   :project/test  {:jvm-opts       ["-Dconf=test-config.edn"]
                   :resource-paths ["env/test/resources"]}

   :profiles/test {}})
