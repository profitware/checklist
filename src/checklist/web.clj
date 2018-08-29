(ns checklist.web
  (:require [compojure.core :as compojure]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.basic-authentication :as basic-authentication]
            [ring.middleware.reload :as reload]
            [ring.middleware.json :as json]
            [ring.util.response :as response]
            [hiccup.page :as page]
            [checklist.db :as db]
            [checklist.schedule :as schedule]
            [checklist.schedule-spec :as schedule-spec])
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
(def page-cards "cards")
(def page-schedule "schedule")


(defn get-menu [page-name]
  [:ul {:class "nav navbar-nav navbar-primary"}
   (menu page-today "Today" "/")
   (menu page-cards "Cards")
   (menu page-schedule "Schedule")
   (comment [:li {:class "dropdown"}
             [:a {:href "#0"
                  :class "dropdown-toggle"
                  :data-toggle "dropdown"}
              "Settings"
              [:b {:class "caret"}]]
             [:ul {:class "dropdown-menu"}]])])


(defn get-script [page-name auth]
  [:script (str (when (= (.contains [page-cards page-schedule]
                                    page-name))
                  (let [content (if (= page-name page-cards)
                                  (db/get-cards-string)
                                  (schedule/get-schedule-string {:page-name page-name
                                                                 :auth auth}))]
                    (str "$(function () {"
                         "  var contentCodeMirror = CodeMirror(document.getElementById('codemirror'), {"
                         "    value: `" content "`,"
                         "    theme: 'mdn-like',"
                         "    lineNumbers: true,"
                         "    styleActiveLine: true,"
                         "    matchBrackets: true"
                         "  });"
                         "  parinferCodeMirror.init(contentCodeMirror);"
                         "  $('button.close').click(function () { $(this).closest('.alert').hide(); });"
                         "  $('.action-button').click(function () {"
                         "    var payload = {'" page-name "-code': contentCodeMirror.getValue()};"
                         "    $('.alert-danger').hide();"
                         "    $('.alert-success').hide();"
                         "    $.ajax({"
                         "      type: 'POST',"
                         "      url: '" page-name "/ajax',"
                         "      data: JSON.stringify(payload),"
                         "      contentType: 'application/json'"
                         "    }).done(function (data) {"
                         "      if (data['" page-name "-code']) {"
                         "        contentCodeMirror.getValue(data['" page-name  "-code']);"
                         "        $('.alert-success').show();"
                         "      } else if (data['error']) {"
                         "        $('.alert-danger').show();"
                         "      }"
                         "    });"
                         "  });"
                         "});")))
                (str "$(function () {"
                     "  var checkbox_selector = 'input[type=\"checkbox\"]';"
                     "  $(checkbox_selector).click(function () {"
                     "    var $card_pf = $(this).closest('.card-pf'),"
                     "        card_id = $card_pf.attr('id'),"
                     "        payload = {"
                     "          'card-id': card_id,"
                     "          'checked': $card_pf.find(checkbox_selector + ':checked').map(function (i, el) {"
                     "            return $(this).attr('name');"
                     "          }).get()"
                     "        };"
                     "    $.ajax({"
                     "      type: 'POST',"
                     "      url: '" page-name "/ajax',"
                     "      data: JSON.stringify(payload),"
                     "      contentType: 'application/json'"
                     "    }).done(function (data) {"
                     "      var cards = data.cards,"
                     "          checked_count = 0;"
                     "      if (cards) {"
                     "        $(cards).each(function () {"
                     "          if (this['card-id'] == card_id) {"
                     "            $(this['card-checkboxes']).each(function () {"
                     "              var checked_this = this['checkbox-checked'];"
                     "              if (checked_this) {"
                     "                checked_count += 1;"
                     "              }"
                     "              $card_pf.find(checkbox_selector + '#' + this['checkbox-id'])"
                     "                      .attr('checked', checked_this ? 'checked' : undefined);"
                     "            });"
                     "          }"
                     "        });"
                     "      }"
                     "      if (checked_count == $card_pf.find(checkbox_selector).length) {"
                     "        $card_pf.addClass('card-disabled');"
                     "      } else {"
                     "        $card_pf.removeClass('card-disabled');"
                     "      }"
                     "    });"
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
   (for [{card-id :card-id
          card-title :card-title
          card-checkboxes :card-checkboxes} cards]
     [:div {:class "col-xs-6 col-sm-4 col-md-4"}
      [:div {:class (str "card-pf"
                         (when (reduce (fn [acc checkbox]
                                         (and acc (get checkbox :checkbox-checked)))
                                       true
                                       card-checkboxes)
                           " card-disabled"))
             :id card-id}
       [:h2 {:class "card-pf-title"}
        card-title]
       [:div {:class "card-pf-body"}
        [:form {:class "form-horizontal"}
         (for [{checkbox-id :checkbox-id
                checkbox-title :checkbox-title
                checkbox-checked :checkbox-checked
                checkbox-disabled :checkbox-disabled} card-checkboxes]
           (get-checkbox checkbox-id checkbox-title checkbox-checked checkbox-disabled))]]]])])


(defn get-editor [page-name]


  [:div {:class "col-xs-12 col-sm-9"}
   [:p {:class "pull-right visible-xs"}
    [:button {:class "btn btn-primary btn-xs"
              :type "button"
              :data-toggle "offcanvas"}
     "Toggle nav"]]

   (when (.contains [page-cards page-schedule]
                    page-name)
     [:div {:class "alert alert-danger alert-dismissable"
            :style "display: none;"}
      [:button {:type "button"
                :class "close"
                :aria-label "Close"}
       [:span {:class "pficon pficon-close"}]]
      [:span {:class "pficon pficon-error-circle-o"}]
      [:strong "Error!"]
      " Your syntax is wrong!"])

   (when (.contains [page-cards page-schedule]
                    page-name)
     [:div {:class "alert alert-success alert-dismissable"
            :style "display: none;"}
      [:button {:type "button"
                :class "close"
                :aria-label "Close"}
       [:span {:class "pficon pficon-close"}]]
      [:span {:class "pficon pficon-ok"}]
      [:strong "Success!"]
      " Your cards are updated now!"])

   [:div {:id "codemirror"}]])


(defn get-sidebar [page-name auth]
  [:div {:id "sidebar"
         :class "col-xs-6 col-sm-3 sidebar-offcanvas"}
   (when (.contains [page-cards page-schedule]
                    page-name)
     [:p (str "Press the button below to run or re-run the job. "
                "Once the job is finished, the contents on the left would be updated.")])
   (when (= page-name page-cards)
     [:button {:class "btn btn-primary action-button"
               :type "button"}
      "Update Cards"])
   (when (= page-name page-schedule)
     [:button {:class "btn btn-primary action-button"
               :type "button"}
      "Update Schedule"])
   [:p {:class "loading-contents"
        :style "display: none;"}
    [:span {:class "spinner spinner-xs spinner-inline"}]
    "\nWait while job is running"]])


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
                 (if (.contains [page-cards page-schedule]
                                page-name)
                   (get-editor page-name)
                   (get-cards (db/get-cards)))
                 (get-sidebar page-name auth)]]

               (when (.contains [page-cards page-schedule]
                                page-name)
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


(defn get-ajax [page-name request]
  (response/response {:hello 1}))


(defn post-ajax [page-name request]
  (cond
    (= page-name page-cards) (let [{input-json :body} request
                                   cards-code (:cards-code input-json)]
                               (if-let [cards-expr (db/expression-of-type :cards cards-code)]
                                 (let [formatted-string (db/upsert-cards-string! cards-code)]
                                   (response/response {:cards-code formatted-string}))
                                 (response/response {:error "Cannot evaluate expression"})))
    (= page-name page-schedule) (let [{input-json :body} request
                                      schedule-code (:schedule-code input-json)]
                                  (if-let [schedule-expr (schedule-spec/evaluate-expr schedule-code)]
                                    (let [formatted-string (schedule/apply-schedule-string! {} schedule-code)]
                                      (response/response {:schedule-code formatted-string}))
                                    (response/response {:error "Cannot evaluate expression"})))
    :else (let [{input-json :body} request
                card input-json
                {checked-ids :checked} card
                old-cards (db/get-cards)
                
                old-card (first (filter #(= (:card-id %)
                                            (:card-id card))
                                        old-cards))]
            (if old-card
              (let [new-card (assoc old-card
                                    :card-checkboxes (reduce (fn [acc old-checkbox]
                                                               (conj acc (if (:disabled old-checkbox)
                                                                           old-checkbox
                                                                           (assoc old-checkbox
                                                                                  :checkbox-checked (.contains checked-ids
                                                                                                               (:checkbox-id old-checkbox))))))
                                                             []
                                                             (:card-checkboxes old-card)))]
                (db/upsert-card! new-card)
                (response/response {:cards [new-card]}))
              (response/response {:cards []})))))


(defmacro get-routes [page-name & url]
  (let [web-url (or (first url)
                    (str "/" page-name))
        page-symbol (symbol (str "page-" page-name))]
    `(list (compojure/GET ~web-url request# (get-page ~page-symbol (get request# :basic-authentication)))
           (compojure/GET (str "/" ~page-name "/ajax") request# (get-ajax ~page-symbol request#))
           (compojure/POST (str "/" ~page-name "/ajax") request# (post-ajax ~page-symbol request#)))))


(defmacro get-all-routes []
  `(compojure/defroutes routes
     ~@(get-routes "today" "/")
     ~@(get-routes "cards")
     ~@(get-routes "schedule")
     (route/resources "/")
     (route/not-found "<h1>Not found</h1>")))


(get-all-routes)


(def app (-> routes
             (json/wrap-json-response)
             (json/wrap-json-body {:keywords? true :bigdecimals? true})
             (basic-authentication/wrap-basic-authentication auth-ok?)
             (reload/wrap-reload)))


(defn main []
  (jetty/run-jetty (reload/wrap-reload app)
                   {:port 3000}))
