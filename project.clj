(defproject zombie-run "0.1.0-SNAPSHOT"
  :description "A simple zombie shooter"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-beta1"]
                 [org.clojure/clojurescript "1.9.946"]
                 [clj-time "0.14.0"]
                 [quil "2.6.0"]
                 [reagent "0.8.0-alpha1"]
                 [com.andrewmcveigh/cljs-time "0.5.0"]]

  :monkeypatch-clojure-test false

  :clean-targets ^{:protect false} [:target-path :compile-path "out" "resources/public/js"]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-doo "0.1.8"]]

  :figwheel {:css-dirs          ["resources/public/css"]
             :open-file-command "open-in-intellij"}

  :doo {:paths {:phantom "node_modules/phantomjs-bin/bin/linux/x64/phantomjs"}}

  :cljsbuild {
              :builds [{:id       "zombie-run"
                        :source-paths ["src"]
                        :figwheel true
                        :compiler {:main       "zombie-run.core"
                                   :asset-path "js/out"
                                   :output-to  "resources/public/js/zombie_run.js"
                                   :output-dir "resources/public/js/out"}}
                       {:id           "test"
                        :source-paths ["test"]
                        :compiler     {:main          zombie-run.runner
                                       :optimizations :none
                                       :output-to     "resources/public/js/tests/all-tests.js"}}]}


  :profiles {:uberjar {:aot      :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]
                       :main     zombie-run.quil}
             :dev     {:dependencies [[org.clojure/test.check "0.9.0"]
                                      [figwheel-sidecar "0.5.15-SNAPSHOT"]]
                       :source-paths ["script"]}}

  :aliases {"cljs-test" ["doo" "phantom" "test" "once"]
            "all-tests" ["do" ["clean"] ["test"] ["cljs-test"]]})
