(defproject checklist "0.1.0-SNAPSHOT"
  :description "Checklist"
  :url "http://github.com/profitware/checklist"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :plugins [[lein-ring "0.12.0"]
            [lein-openshift "0.1.0"]
            [lein-npm "0.6.2"]
            [lein-resource "16.9.1"]
            [lein-bump-version "0.1.6" :exclusions [rewrite-clj]]
            [jonase/eastwood "0.2.5"]
            [lein-cljfmt "0.6.0" :exclusions [org.clojure/tools.reader]]
            [arohner/lein-docker "0.1.4"]]
  :ring {:handler checklist.web/app
         :init checklist.db/init
         :destroy checklist.db/destroy
         :port 8080
         :nrepl {:start? true
                 :port 7888}}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.4.474"]
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.slf4j/slf4j-simple "1.7.25"]
                 [cheshire "5.8.0"]
                 [clj-http "3.9.1" :exclusions [org.apache.httpcomponents/httpclient]]
                 [ring/ring-core "1.6.3"]
                 [ring/ring-jetty-adapter "1.6.3"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-defaults "0.3.2"]
                 [compojure "1.6.1" :exclusions [ring/ring-codec]]
                 [hiccup "1.0.5"]
                 [cljfmt "0.6.0" :exclusions [rewrite-cljs]]
                 [org.apache.httpcomponents/httpclient "4.5.3"]
                 [clj-time "0.14.4"]
                 [datascript "0.16.6"]
                 [io.replikativ/datahike "0.1.2" :exclusions [org.clojure/data.priority-map
                                                              org.clojure/data.codec
                                                              org.clojure/core.cache
                                                              org.clojure/core.memoize
                                                              org.clojure/core.async]]
                 [com.cemerick/friend "0.2.3"]
                 [clojusc/friend-oauth2 "0.2.0" :exclusions [ring
                                                             ring/ring-jetty-adapter
                                                             com.taoensso/truss
                                                             com.taoensso/encore]]
                 [factual/timely "0.0.3"]
                 [environ "1.1.0"]]
  :cljfmt {:remove-consecutive-blank-lines? false}
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
  :openshift {:domains ["todo.profitware.tech"]
              :namespace "checklist"
              :app "checklist-alpha"
              :env {"CHECKLIST_ADMIN_USER" nil
                    "CHECKLIST_ADMIN_PASS" nil
                    "CHECKLIST_DATABASE_URI" "datahike:file:///var/lib/checklist/data"
                    "CHECKLIST_DOMAIN" "todo.profitware.tech"
                    "CHECKLIST_SESSION_KEY" nil
                    "CHECKLIST_GITHUB_CLIENT_ID" nil
                    "CHECKLIST_GITHUB_SECRET" nil}
              :recreate true}
  :docker {:repo "profitware/checklist"
           :ports {8080 8080}
           :env #{"CHECKLIST_ADMIN_USER"
                  "CHECKLIST_ADMIN_PASSWORD"
                  "CHECKLIST_GITHUB_CLIENT_ID"
                  "CHECKLIST_GITHUB_SECRET"
                  "CHECKLIST_DOMAIN"
                  "CHECKLIST_SESSION_KEY"
                  "CHECKLIST_DATABASE_URI"}}
  :main ^:skip-aot checklist.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[ring/ring-devel "1.6.3"]
                                  [org.clojure/tools.namespace "0.2.11"]]
                   :ring {:handler dev/reloaded-app}
                   :source-paths ["src" "src-dev"]}})
