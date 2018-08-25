(defproject checklist/checklist-core "0.1.0"
  :description "Checklist Core Library"
  :url "http://github.com/profitware/checklist"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-cljfmt "0.6.0"]]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [cljfmt "0.6.0"]
                 [factual/timely "0.0.3"]
                 [org.clojure/tools.namespace "0.2.11"]]
  :target-path "target/%s"
  :main ^:skip-aot checklist-core.core
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[org.clojure/test.check "0.9.0"]]}})
