(defproject checklist/checklist-web "0.1.0"
  :description "Checklist"
  :url "http://github.com/profitware/checklist"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :plugins [[lein-ring "0.12.0"]
            [lein-npm "0.6.2"]
            [lein-resource "16.9.1"]
            [lein-bump-version "0.1.6"]
            [arohner/lein-docker "0.1.4"]]
  :ring {:handler checklist.web/app
         :nrepl {:start? true
                 :port 9998}}
  :dependencies [[flower "0.4.2-SNAPSHOT"]
                 [org.clojure/core.async "0.4.474"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/data.codec "0.1.0"]
                 [ring/ring-jetty-adapter "1.6.3"]
                 [ring/ring-devel "1.6.3"]
                 [ring-basic-authentication "1.0.5"]
                 [http-kit "2.2.0"]
                 [compojure "1.6.1"]
                 [hiccup "1.0.5"]]
  :npm {:dependencies [[patternfly "3.48.2"]
                       [bootstrap "3.3.7"]
                       [jquery "3.3.1"]]
        :root "resources"}
  :resource {:resource-paths
             [["resources/node_modules/patternfly/dist/js" {:includes [#".*(\.min)?\.js(\.map)?"]
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
                                                               :extra-values {}}]]
             :skip-stencil [ #"resources/node_modules/patternfly/dist/(img|fonts)/.*" ]}
  :main ^:skip-aot checklist.web
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
