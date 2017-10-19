(defproject zombie-run "0.1.0-SNAPSHOT"
  :description "A simple zombie shooter"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-beta1"]
                 [org.clojure/clojurescript "1.9.946"]
                 [clj-time "0.14.0"]
                 [quil "2.6.0"]
                 [reagent "0.8.0-alpha1"]
                 [com.andrewmcveigh/cljs-time "0.5.1"]
                 [ring "1.6.2"]
                 [ring/ring-defaults "0.3.1"]
                 [bk/ring-gzip "0.2.1"]
                 [compojure "1.6.0"]]

  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :test-paths ["test/clj" "test/cljs" "test/cljc"]

  :monkeypatch-clojure-test false

  :clean-targets ^{:protect false} [:target-path :compile-path "out" "resources/public/js"]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-doo "0.1.8"]]

  :figwheel {:css-dirs          ["resources/public/css"]
             :open-file-command "open-in-intellij"}

  :cljsbuild {
              :builds [{:id           "zombie-run"
                        :source-paths ["src/cljs" "src/cljc"]
                        :figwheel     true
                        :compiler     {:main                 "zombie-run.core"
                                       :asset-path           "js/out"
                                       :output-to            "resources/public/js/zombie_run.js"
                                       :output-dir           "resources/public/js/out"
                                       :source-map-timestamp true}}
                       {:id           "test"
                        :source-paths ["test/cljs"]
                        :compiler     {:main          zombie-run.runner
                                       :optimizations :none
                                       :output-to     "resources/public/js/tests/all-tests.js"}}
                       {:id           "min"
                        :source-paths ["src/cljs" "src/cljc"]
                        :jar          true
                        :compiler     {:main                 zombie-run.core
                                       :output-to            "resources/public/js/zombie_run.js"
                                       :output-dir           "target"
                                       :source-map-timestamp true
                                       :optimizations        :advanced
                                       :pretty-print         false}}]}


  :profiles {:uberjar {:aot          :all
                       :jvm-opts     ["-Dclojure.compiler.direct-linking=true"]
                       :main         zombie-run.server
                       :source-paths ^:replace ["src/clj" "src/cljc"]
                       :prep-tasks   ["compile" ["cljsbuild" "once" "min"]]
                       :hooks        []
                       :omit-source  true}

             :dev     {:dependencies [[org.clojure/test.check "0.9.0"]
                                      [figwheel-sidecar "0.5.15-SNAPSHOT"]]
                       :source-paths ["script"]}}

  :aliases {"cljs-test" ["doo" "phantom" "test" "once"]
            "all-tests" ["do" ["clean"] ["test"] ["cljs-test"]]})
