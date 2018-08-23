(ns checklist.web
  (:require [compojure.core :as compojure]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.basic-authentication :as basic-authentication]
            [ring.middleware.reload :as reload]
            [hiccup.page :as page]
            [checklist-core.core :as checklist]))


(defn auth-ok? [user pass]
  true)


(def head
  [:head
   [:title "Checklist"]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
   (page/include-css "/css/patternfly.min.css")
   (page/include-css "/css/patternfly-additions.min.css")
   (page/include-css "/css/codemirror.css")
   (page/include-css "/css/mdn-like.css")
   [:style (str "html {" \newline
                "  overflow-y: scroll;" \newline
                "}" \newline
                ".navbar {" \newline
                "  margin-bottom: 0;" \newline
                "}" \newline
                ".CodeMirror {" \newline
                "  border: 1px solid #eee;" \newline
                "  height: auto;" \newline
                "}")]])


(defmacro menu [page-name page-title & page-href]
  `[:li {:class (when (= ~'page-name ~page-name)
                  "active")}
    [:a {:href (or ~@page-href
                   (str "/" ~page-name))}
     ~page-title]])


(def page-today "today")
(def page-schedule "schedule")


(defn get-menu [page-name]
  [:ul {:class "nav navbar-nav navbar-primary"}
   (menu page-today "Today" "/")
   [:li {:class "dropdown"}
    [:a {:href "#0"
         :class "dropdown-toggle"
         :data-toggle "dropdown"}
     "Settings"
     [:b {:class "caret"}]]
    [:ul {:class "dropdown-menu"}
     (menu page-schedule "Schedule")]]])


(defn get-script [page-name]
  (when (= page-name page-schedule)
    (let [schedule (checklist/get-default-data)
          schedule-string (str schedule)]
      [:script (str "$(function () {"
                    "  var scheduleCodeMirror = CodeMirror(document.getElementById('codemirror'), {"
                    "    value: '" schedule "',"
                    "    theme: 'mdn-like',"
                    "    lineNumbers: true,"
                    "    styleActiveLine: true,"
                    "    matchBrackets: true"
                    "  });"
                    "  parinferCodeMirror.init(scheduleCodeMirror);"
                    "});")])))


(defn get-contents-by-page [page-name]
  "Hello, world! This is a test!")


(defn get-cards []
  [:div {:class "col-xs-12 col-sm-9"}
   [:p {:class "pull-right visible-xs"}
    [:button {:class "btn btn-primary btn-xs"
              :type "button"
              :data-toggle "offcanvas"}
     "Toggle nav"]]

   [:div {:class "col-xs-6 col-sm-4 col-md-4"}
    [:div {:class "card-pf"}
     [:h2 {:class "card-pf-title"}
      "Some Checklist"]
     [:div {:class "card-pf-body"}
      [:form {:class "form-horizontal"}
       [:div {:class "form-group"}
        [:label {:class "col-sm-9 control-label"
                 :for "checkbox1"}
         "Hello! This is a test!"]
        [:div {:class "col-sm-3"}
         [:input {:type "checkbox"
                  :id "checkbox1"
                  :name "checkbox1"
                  :class "form-control"}]]]

       [:div {:class "form-group"}
        [:label {:class "col-sm-9 control-label"
                 :for "checkbox2"}
         "Wow!"]
        [:div {:class "col-sm-3"}
         [:input {:type "checkbox"
                  :id "checkbox2"
                  :name "checkbox2"
                  :checked "checked"
                  :disabled "disabled"
                  :class "form-control"}]]]]]]]])


(defn get-editor []
  [:div {:class "col-xs-12 col-sm-9"}
   [:p {:class "pull-right visible-xs"}
    [:button {:class "btn btn-primary btn-xs"
              :type "button"
              :data-toggle "offcanvas"}
     "Toggle nav"]]

   [:div {:id "codemirror"}]])


(defn get-page [page-name auth]
  (page/html5 head
              [:body {:class "cards-pf"}
               [:nav {:class "navbar navbar-inverse"}
                [:div {:class "container-fluid"}
                 [:div {:class "navbar-header"}
                  [:button {:class "navbar-toggle collapsed"
                            :type "button"
                            :data-toggle "collapse"
                            :data-target "#navbar"
                            :aria-expanded "false"
                            :aria-controls "navbar"}
                   [:span {:class "sr-only"} "Toggle navigation"]
                   [:span {:class "icon-bar"}]
                   [:span {:class "icon-bar"}]
                   [:span {:class "icon-bar"}]]
                  [:a {:class "navbar-brand"
                       :href "/"}
                   "Checklist"]]
                 [:div {:id "navbar"
                        :class "collapse navbar-collapse"}
                  (get-menu page-name)]]]

               [:div {:class "container-fluid"
                      :style "display: none;"}
                [:div {:class "row toolbar-pf"}
                 [:div {:class "col-sm-12"}
                  [:form {:class "toolbar-pf-actions"}
                   [:div {:class "form-group"}
                    [:button {:class "btn btn-default"
                              :type "button"}
                     "Action"]]]]]]

               [:div {:class "container-fluid container-cards-pf"}
                [:div {:class "row row-offcanvas row-offcanvas-right row-cards-pf"}
                 (if (= page-name "schedule")
                   (get-editor)
                   (get-cards))
                 [:div {:id "sidebar"
                        :class "col-xs-6 col-sm-3 sidebar-offcanvas"}
                  [:p (str "Sidebar contents here.")]]]]

               (when (= page-name page-schedule)
                 [:div {:style "display: hidden;"}
                  (page/include-js "/js/codemirror.js")
                  (page/include-js "/js/parinfer.js")
                  (page/include-js "/js/parinfer-codemirror.js")
                  (page/include-js "/js/clojure.js")])
               (page/include-js "/js/jquery.min.js")
               (page/include-js "/js/bootstrap.min.js")
               (page/include-js "/js/patternfly.min.js")
               (page/include-js "/js/patternfly-functions.min.js")
               (get-script page-name)]))


(defmacro get-routes [page-name & url]
  (let [web-url (or (first url)
                    (str "/" page-name))
        page-symbol (symbol (str "page-" page-name))]
    `(list (compojure/GET ~web-url request# (get-page ~page-symbol (get request# :basic-authentication)))
           (compojure/GET (str "/" ~page-name "/ajax") request# (get-page ~page-symbol (get request# :basic-authentication))))))


(defmacro get-all-routes []
  `(compojure/defroutes routes
     ~@(get-routes "today" "/")
     ~@(get-routes "schedule")
     (route/resources "/")
     (route/not-found "<h1>Not found</h1>")))


(get-all-routes)


(def app (-> routes
             (basic-authentication/wrap-basic-authentication auth-ok?)
             (reload/wrap-reload)))


(defn main []
  (jetty/run-jetty (reload/wrap-reload app)
                   {:port 3000}))
