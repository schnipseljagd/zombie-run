(defproject zombie-run "0.1.0-SNAPSHOT"
  :description "A simple zombie shooter"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-beta1"]
                 [clj-time "0.14.0"]
                 [quil "2.6.0"]]

  :monkeypatch-clojure-test false ; See https://github.com/technomancy/leiningen/issues/2173

  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]
                       :main zombie-run.quil}
             :dev {:dependencies [[org.clojure/test.check "0.9.0"]]}})
