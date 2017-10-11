(defproject zombie-run "0.1.0-SNAPSHOT"
  :description "A simple zombie shooter"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-beta1"]
                 [org.clojure/clojurescript "1.9.946"]
                 [clj-time "0.14.0"]
                 [quil "2.6.0"]
                 [reagent "0.8.0-alpha1"]]

  :monkeypatch-clojure-test false

  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :test-paths ["test/clj" "test/cljc" "src/cljs"]
  :clean-targets ^{:protect false} [:target-path :compile-path "resources/public/js"]

  :plugins [[lein-cljsbuild "1.1.7"]]

  :figwheel {:css-dirs ["resources/public/css"]
             :open-file-command "open-in-intellij"}

  :cljsbuild {
              :builds [{:id           "example"
                        :source-paths ["src/cljs/"]
                        :figwheel     true
                        :compiler     {:main       "example.core"
                                       :asset-path "js/out"
                                       :output-to  "resources/public/js/example.js"
                                       :output-dir "resources/public/js/out"}}]}


  :profiles {:uberjar {:aot      :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]
                       :main     zombie-run.quil}
             :dev     {:dependencies [[org.clojure/test.check "0.9.0"]
                                      [figwheel-sidecar "0.5.15-SNAPSHOT"]]
                       :source-paths ["script"]}})
