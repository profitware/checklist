(defproject checklist "0.1.0"
  :description "Checklist Library"
  :url "http://github.com/profitware/checklist"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :plugins [[lein-sub "0.3.0"]
            [lein-bump-version "0.1.6"]]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [checklist/checklist-web "0.1.1"]]
  :aliases {"test" ["sub" "lint-and-test-all"]
            "bump-all" ["do" ["bump-version"] ["sub" "bump-version"]]
            "deploy-all" ["do" ["sub" "deploy" "clojars"] ["deploy" "clojars"]]}
  :sub ["checklist-web"]
  :profiles {:1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :dev {:dependencies [[org.clojure/tools.namespace "0.2.11"]]}})
