(defproject checklist "0.1.0-SNAPSHOT"
  :description "Checklist"
  :url "http://github.com/profitware/checklist"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :plugins [[lein-ring "0.12.0"]
            [lein-npm "0.6.2"]
            [lein-resource "16.9.1"]
            [lein-bump-version "0.1.6"]
            [arohner/lein-docker "0.1.4"]
            [lein-cljfmt "0.6.0"]
            [arohner/lein-docker "0.1.4"]]
  :ring {:handler checklist.web/reloaded-app
         :init checklist.web/init
         :port 5000
         :nrepl {:start? true
                 :port 9998}}
  :dependencies [[org.clojure/core.async "0.4.474"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.slf4j/slf4j-simple "1.7.25"]
                 [cheshire "5.8.0"]
                 [clj-http "3.9.1"]
                 [ring/ring-jetty-adapter "1.6.3"]
                 [ring/ring-devel "1.6.3"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-defaults "0.3.2"]
                 [http-kit "2.2.0"]
                 [compojure "1.6.1"]
                 [hiccup "1.0.5"]
                 [cljfmt "0.6.0"]
                 [org.apache.httpcomponents/httpclient "4.5.3"]
                 [clj-time "0.14.4"]
                 [datascript "0.16.6"]
                 [com.cemerick/friend "0.2.3"]
                 [clojusc/friend-oauth2 "0.2.0"]
                 [factual/timely "0.0.3"]
                 [environ "1.1.0"]]
  :jvm-opts ["-Dorg.slf4j.simpleLogger.defaultLogLevel=warn"
             "-Dorg.slf4j.simpleLogger.showDateTime=true"
             "-Dorg.slf4j.simpleLogger.showThreadName=false"]
  :npm {:dependencies [[patternfly "3.48.2"]
                       [bootstrap "3.3.7"]
                       [jquery "3.3.1"]
                       [codemirror "5.39.2"]
                       [parinfer-codemirror "1.4.2"]]
        :root "resources"}
  :resource {:resource-paths
             [["resources/src/js" {:includes [#".*(\.min)?\.js(\.map)?"]
                                   :excludes []
                                   :target-path "resources/public/js"
                                   :extra-values {}}]
              ["resources/src/css" {:includes [#".*(\.min)?\.css(\.map)?"]
                                    :excludes []
                                    :target-path "resources/public/css"
                                    :extra-values {}}]
              ["resources/node_modules/patternfly/dist/js" {:includes [#".*(\.min)?\.js(\.map)?"]
                                                            :excludes []
                                                            :target-path "resources/public/js"
                                                            :extra-values {}}]
              ["resources/node_modules/bootstrap/dist/js" {:includes [#".*(\.min)?\.js(\.map)?"]
                                                           :excludes []
                                                           :target-path "resources/public/js"
                                                           :extra-values {}}]
              ["resources/node_modules/jquery/dist" {:includes [#".*(\.min)?\.js(\.map)?"]
                                                     :excludes []
                                                     :target-path "resources/public/js"
                                                     :extra-values {}}]
              ["resources/node_modules/codemirror/lib" {:includes [#".*(\.min)?\.js(\.map)?"]
                                                        :excludes []
                                                        :target-path "resources/public/js"
                                                        :extra-values {}}]
              ["resources/node_modules/parinfer" {:includes [#".*(\.min)?\.js(\.map)?"]
                                                  :excludes []
                                                  :target-path "resources/public/js"
                                                  :extra-values {}}]
              ["resources/node_modules/parinfer-codemirror" {:includes [#".*(\.min)?\.js(\.map)?"]
                                                             :excludes []
                                                             :target-path "resources/public/js"
                                                             :extra-values {}}]
              ["resources/node_modules/codemirror/addon/edit" {:includes [#".*(\.min)?\.js(\.map)?"]
                                                               :excludes []
                                                               :target-path "resources/public/js"
                                                               :extra-values {}}]
              ["resources/node_modules/codemirror/mode/clojure" {:includes [#".*(\.min)?\.js(\.map)?"]
                                                                 :excludes []
                                                                 :target-path "resources/public/js"
                                                                 :extra-values {}}]
              ["resources/node_modules/patternfly/dist/css" {:includes [#".*\.min\.css"]
                                                             :excludes []
                                                             :target-path "resources/public/css"
                                                             :extra-values {}}]
              ["resources/node_modules/patternfly/dist/img" {:includes [#".*\.(svg|png|ico|gif|jpg)"]
                                                             :excludes []
                                                             :target-path "resources/public/img"
                                                             :extra-values {}}]
              ["resources/node_modules/patternfly/dist/fonts" {:includes [#".*\.(otf|eot|svg|ttf|woff2?)"]
                                                               :excludes []
                                                               :target-path "resources/public/fonts"
                                                               :extra-values {}}]
              ["resources/node_modules/codemirror/lib" {:includes [#".*\.css"]
                                                        :excludes []
                                                        :target-path "resources/public/css"
                                                        :extra-values {}}]
              ["resources/node_modules/codemirror/theme" {:includes [#".*\.css"]
                                                          :excludes []
                                                          :target-path "resources/public/css"
                                                          :extra-values {}}]]
             :skip-stencil [ #"resources/node_modules/patternfly/dist/(img|fonts)/.*" ]}
  :docker {:repo "profitware/checklist"
           :ports {5000 5000}
           :env #{"CHECKLIST_ADMIN_USER"
                  "CHECKLIST_ADMIN_PASSWORD"
                  "CHECKLIST_GITHUB_CLIENT_ID"
                  "CHECKLIST_GITHUB_SECRET"
                  "CHECKLIST_DOMAIN"}}
  :main ^:skip-aot checklist.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
