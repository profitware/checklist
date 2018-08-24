(ns checklist.web
  (:require [compojure.core :as compojure]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.basic-authentication :as basic-authentication]
            [ring.middleware.reload :as reload]
            [hiccup.page :as page]
            [checklist-core.core :as checklist])
  (:gen-class))


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
                ".card-disabled {" \newline
                "  opacity: 0.2;" \newline
                "  color: #aaaaaa;" \newline
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
   (menu page-schedule "Schedule")
   (comment [:li {:class "dropdown"}
             [:a {:href "#0"
                  :class "dropdown-toggle"
                  :data-toggle "dropdown"}
              "Settings"
              [:b {:class "caret"}]]
             [:ul {:class "dropdown-menu"}]])])


(defn get-script [page-name auth]
  [:script (str (when (= page-name page-schedule)
                  (let [schedule (checklist/get-data-string {:page-name page-name
                                                             :auth auth})]
                    (str "$(function () {"
                         "  var scheduleCodeMirror = CodeMirror(document.getElementById('codemirror'), {"
                         "    value: `" schedule "`,"
                         "    theme: 'mdn-like',"
                         "    lineNumbers: true,"
                         "    styleActiveLine: true,"
                         "    matchBrackets: true"
                         "  });"
                         "  parinferCodeMirror.init(scheduleCodeMirror);"
                         "});")))
                (str "$(function () {"
                     "  var checkbox_selector = 'input[type=\"checkbox\"]';"
                     "  $(checkbox_selector).click(function () {"
                     "    var $card_pf = $(this).closest('.card-pf');"
                     "    if ($card_pf.find(checkbox_selector + ':checked').length == $card_pf.find(checkbox_selector).length) {"
                     "      $card_pf.addClass('card-disabled');"
                     "    } else {"
                     "      $card_pf.removeClass('card-disabled');"
                     "    }"
                     "  });"
                     "});"))])


(defn get-contents-by-page [page-name]
  "Hello, world! This is a test!")


(defn get-checkbox [checkbox-id checkbox-title checkbox-checked checkbox-disabled]
  [:div {:class "form-group"}
   [:label {:class "col-sm-9 control-label"
            :for checkbox-id}
    checkbox-title]
   [:div {:class "col-sm-3"}
    [:input (into {:type "checkbox"
                   :id checkbox-id
                   :name checkbox-id
                   :class "form-control"}
                  [(when checkbox-checked
                     [:checked "checked"])
                   (when checkbox-disabled
                     [:disabled "disabled"])])]]])


(defn get-cards [cards]
  [:div {:class "col-xs-12 col-sm-9"}
   [:p {:class "pull-right visible-xs"}
    [:button {:class "btn btn-primary btn-xs"
              :type "button"
              :data-toggle "offcanvas"}
     "Toggle nav"]]
   (for [{card-title :card-title
          card-checkboxes :card-checkboxes} cards]
     [:div {:class "col-xs-6 col-sm-4 col-md-4"}
      [:div {:class (str "card-pf"
                         (when (reduce (fn [acc checkbox]
                                         (and acc (get checkbox :checkbox-checked)))
                                       true
                                       card-checkboxes)
                           " card-disabled"))}
       [:h2 {:class "card-pf-title"}
        card-title]
       [:div {:class "card-pf-body"}
        [:form {:class "form-horizontal"}
         (for [{checkbox-id :checkbox-id
                checkbox-title :checkbox-title
                checkbox-checked :checkbox-checked
                checkbox-disabled :checkbox-disabled} card-checkboxes]
           (get-checkbox checkbox-id checkbox-title checkbox-checked checkbox-disabled))]]]])])


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
                   (get-cards (checklist/get-cards-for-query {:page-name page-name
                                                              :auth auth})))
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
               (get-script page-name auth)]))


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
