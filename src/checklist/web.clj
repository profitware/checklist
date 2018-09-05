(ns checklist.web
  (:require [clojure.tools.logging :as log]
            [compojure.core :as compojure]
            [compojure.route :as route]
            [environ.core :as environ]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.anti-forgery :as anti-forgery]
            [ring.middleware.cookies :as cookies]
            [ring.middleware.params :as params]
            [ring.middleware.reload :as reload]
            [ring.middleware.session :as session]
            [ring.middleware.json :as json]
            [ring.middleware.defaults :as defaults]
            [ring.util.anti-forgery :as util-anti-forgery]
            [ring.util.response :as response]
            [hiccup.page :as page]
            [hiccup.util :as util]
            [timely.core :as timely]
            [cemerick.friend :as friend]
            [cemerick.friend.credentials :as credentials]
            [cemerick.friend.workflows :as workflows]
            [checklist.db :as db]
            [checklist.github :as github])
  (:gen-class))


(derive ::admin ::user)


(def ^:dynamic *tenant* "default")


(def head
  [:head
   [:title (str "Checklist | " *tenant*)]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
   (page/include-css "/css/patternfly.min.css")
   (page/include-css "/css/patternfly-additions.min.css")
   (page/include-css "/css/codemirror.css")
   (page/include-css "/css/mdn-like.css")
   (page/include-css "/css/main.css")])


(defmacro menu [page-name page-title & page-href]
  `[:li {:class (when (= ~'page-name ~page-name)
                  "active")}
    [:a {:href (or ~@page-href
                   (str "/" ~page-name))}
     ~page-title]])


(def page-today "today")
(def page-cards "cards")
(def page-schedule "schedule")
(def page-login "login")
(def page-logout "logout")
(def page-notfound "notfound")


(defn get-menu [page-name]
  [:ul {:class "nav navbar-nav navbar-primary"}
   (menu page-today "Today" "/")
   (menu page-cards "Cards")
   (menu page-schedule "Schedule")])


(defn get-menu-login [page-name]
  [:ul {:class "nav navbar-nav navbar-right"}
   (menu page-login "Sign in")])


(defn get-menu-logout [page-name]
  [:ul {:class "nav navbar-nav navbar-right"}
   (menu page-logout (str "Logout [" *tenant* "]"))])


(defn get-script [page-name]
  [:script (if-let [content (condp = page-name
                              page-cards (db/get-cards-string *tenant*)
                              page-schedule (db/get-schedule-string *tenant*)
                              nil)]
             (str "initApp($, '" page-name "', '" anti-forgery/*anti-forgery-token* "', `" (clojure.string/replace (util/escape-html content) #"`" "'") "`);")
             (str "initApp($, '" page-name "', '" anti-forgery/*anti-forgery-token* "');"))])


(defn- checkbox-checked? [{checkbox-checked :checkbox-checked
                           checkbox-disabled :checkbox-disabled}]
  (or (and (not checkbox-disabled)
           checkbox-checked)
      (and checkbox-disabled
           (if (boolean? checkbox-checked)
             checkbox-checked
             (db/get-context-value *tenant* checkbox-checked)))))


(defn get-checkbox [checkbox-id checkbox-title checkbox-checked checkbox-disabled]
  [:div {:class "form-group"}
   [:label {:class "col-sm-9 control-label"
            :for checkbox-id}
    (util/escape-html checkbox-title)]
   [:div {:class "col-sm-3"}
    [:input (into {:type "checkbox"
                   :id checkbox-id
                   :name checkbox-id
                   :class "form-control"}
                  [(when (checkbox-checked? {:checkbox-checked checkbox-checked
                                             :checkbox-disabled checkbox-disabled}) 
                     [:checked "checked"])
                   (when checkbox-disabled
                     [:disabled "disabled"])])]]])


(defn get-empty-state [cards]
  [:div {:class "blank-slate-pf"
         :id "blank"
         :style (when-not (reduce (fn [acc card]
                                    (and acc
                                         (:card-hidden card)))
                                  true
                                  cards)
                  "display: none;")}
   [:div {:class "blank-slate-pf-icon"}
    [:span {:class "pficon pficon pficon-add-circle-o"}]]
   [:h1 "No Cards for Today"]
   [:p "Well, it seems that you don't have anything to do for today. Or you just haven't added anything yet."]
   [:p "In any case, you may perform some actions to check the reasons for this."]
   [:div {:class "blank-slate-pf-main-action"}
    [:a {:class "btn btn-primary btn-lg"
         :href "cards"}
     "Add Cards"]]
   [:div {:class "blank-slate-pf-secondary-action"}
    [:a {:class "btn btn-default"
         :href "schedule"}
     "Check Schedules"]]])


(defn get-cards [cards]
  [:div {:class "col-xs-12 col-sm-9 cards-content"}
   (get-empty-state cards)
   (for [{card-id :card-id
          card-title :card-title
          card-hidden :card-hidden
          card-highlighted :card-highlighted
          card-checkboxes :card-checkboxes} cards]
     [:div {:class "col-xs-6 col-sm-4 col-md-4"}
      [:div {:class (str "card-pf"
                         (when (reduce (fn [acc checkbox]
                                         (let [checked (get checkbox :checkbox-checked)]
                                           (and acc (or (= checked true)
                                                        (and (keyword? checked)
                                                             (db/get-context-value *tenant* checked))))))
                                       true
                                       card-checkboxes)
                           " card-disabled")
                         (when card-highlighted
                           " card-highlighted"))
             :style (when card-hidden
                      "display: none;")
             :id card-id}
       [:h2 {:class "card-pf-title"}
        (util/escape-html card-title)
        [:button {:type "button"
                  :class "highlight-card"
                  :aria-label "Highlight"}
         [:span {:class "pficon pficon-thumb-tack-o"}]]
        [:button {:type "button"
                  :class "hide-card close"
                  :aria-label "Close"}
         [:span {:class "pficon pficon-close"}]]]
       [:div {:class "card-pf-body"}
        [:form {:class "form-horizontal"}
         (for [{checkbox-id :checkbox-id
                checkbox-title :checkbox-title
                checkbox-checked :checkbox-checked
                checkbox-disabled :checkbox-disabled} card-checkboxes]
           (get-checkbox checkbox-id checkbox-title checkbox-checked checkbox-disabled))]]]])])


(defn get-editor [page-name]
  [:div {:class "col-xs-12 col-sm-9"}
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
      (condp = page-name
        page-cards " Your cards are updated now!"
        page-schedule " Your schedule is updated now!")])

   [:div {:id "codemirror"}]])


(defn get-sidebar [page-name]
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
    "\nWait while job is running"]
   (when (= page-name page-today)
     (let [tenant-cards (db/get-cards *tenant*)
           hidden-tenant-cards (filter :card-hidden tenant-cards)]
       (when-not (empty? tenant-cards)
         [:div
          [:p {:class "hidden-message"
               :style (when (empty? hidden-tenant-cards)
                        "display: none;")}
           (str "The following cards are hidden:")]
          [:div
           (for [card tenant-cards]
             [:button {:class (str "btn btn-default show-card")
                       :data-card-id (:card-id card)
                       :style (when-not (:card-hidden card)
                                "display: none;")
                       :type "button"}
              (util/escape-html (:card-title card))])]])))])


(defn get-login-form [request]
  [:div {:class "col-xs-12 col-sm-9"}
   (when (get-in request [:params :login_failed])
     [:div {:class "alert alert-danger alert-dismissable"}
      [:button {:type "button"
                :class "close"
                :aria-label "Close"}
       [:span {:class "pficon pficon-close"}]]
      [:span {:class "pficon pficon-error-circle-o"}]
      [:strong "Error!"]
      " Wrong login or password!"])

   [:form {:class "form-horizontal"
           :accept-charset "UTF-8"
           :method "POST"}
    (util-anti-forgery/anti-forgery-field)
    [:div {:class "form-group"}
     [:label {:class "col-sm-3 control-label"
              :for "username"}
      "Username"]
     [:div {:class "col-sm-6"}
      [:input {:type "text"
               :id "username"
               :name "username"
               :class "form-control"
               :autofocus "autofocus"
               :autocapitalize "off"
               :autocorrect "off"
               :autocomplete "off"}]]]
    [:div {:class "form-group"}
     [:label {:class "col-sm-3 control-label"
              :for "password"}
      "Password"]
     [:div {:class "col-sm-6"}
      [:input {:type "password"
               :id "password"
               :name "password"
               :class "form-control"
               :autocomplete "off"}]]]
    [:div {:style "padding-top: 10px; padding-bottom: 10px;"
           :class "row"}
     [:div {:class "col-sm-6 col-sm-offset-3"}
      [:span
       [:button {:type "submit"
                 :id "submit"
                 :class "btn btn-primary"
                 :data-disable-with "Signing in..."}
        "Sign in"]
       " "]
      (when github/github-client-id
        [:span
         [:a {:href "/login/github"
              :class "btn btn-default"}
          "Sign in using GitHub"]])]]]])


(defn get-not-found []
  [:div {:class "blank-slate-pf"}
   [:h1 "Page not found"]])


(defn get-page [page-name request]
  (let [auth (identity request)]
    (page/html5 head
                [:body {:class "cards-pf"}
                 [:nav {:class "navbar navbar-inverse"
                        :role "navigation"}
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
                    (get-menu page-name)
                    (if (friend/authorized? #{::user} auth)
                      (get-menu-logout page-name)
                      (get-menu-login page-name))]]]

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
                   (cond
                     (.contains [page-cards page-schedule] page-name) (get-editor page-name)
                     (= page-name page-login) (get-login-form request)
                     (= page-name page-today) (let [tenant-cards (db/get-cards *tenant*)]
                                                (get-cards tenant-cards))
                     :else (get-not-found))
                   (get-sidebar page-name)]]

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
                 (page/include-js "/js/main.js")
                 (get-script page-name)])))


(defn get-ajax [page-name request]
  (response/response (if (= page-name page-today)
                       {:cards (map #(assoc %
                                            :card-title (util/escape-html (:card-title %))
                                            :card-checkboxes (reduce (fn [acc checkbox]
                                                                       (conj acc
                                                                             (assoc checkbox
                                                                                    :checkbox-title (util/escape-html (:checkbox-title checkbox))
                                                                                    :checkbox-checked (checkbox-checked? checkbox))))
                                                                     []
                                                                     (:card-checkboxes %)))
                                    (db/get-cards *tenant*))}
                       (response/not-found (get-page page-notfound request)))))


(defn post-ajax [page-name request]
  (cond
    (= page-name page-cards) (let [{input-json :body} request
                                   cards-code (:cards-code input-json)]
                               (if-let [cards-expr (db/expression-of-type :cards cards-code)]
                                 (let [formatted-string (db/upsert-cards-string! *tenant* cards-code)]
                                   (response/response {:cards-code formatted-string}))
                                 (response/response {:error "Cannot evaluate expression"})))
    (= page-name page-schedule) (let [{input-json :body} request
                                      schedule-code (:schedule-code input-json)]
                                  (if-let [schedule-expr (db/expression-of-type :schedule schedule-code)]
                                    (let [formatted-string (db/upsert-schedule-string! *tenant* schedule-code)]
                                      (response/response {:schedule-code formatted-string}))
                                    (response/response {:error "Cannot evaluate expression"})))
    :else (let [{input-json :body} request
                card input-json
                {checked-ids :checked
                 hide-card :hide
                 highlight-card :highlight} card
                old-cards (db/get-cards *tenant*)
                old-card (first (filter #(= (:card-id %)
                                            (:card-id card))
                                        old-cards))]
            (if old-card
              (let [new-card (assoc old-card
                                    :card-hidden (boolean hide-card)
                                    :card-highlighted (boolean highlight-card)
                                    :card-checkboxes (reduce (fn [acc old-checkbox]
                                                               (conj acc (if (:checkbox-disabled old-checkbox)
                                                                           old-checkbox
                                                                           (assoc old-checkbox
                                                                                  :checkbox-checked (.contains checked-ids
                                                                                                               (:checkbox-id old-checkbox))))))
                                                             []
                                                             (:card-checkboxes old-card)))]
                (db/upsert-card! *tenant* new-card)
                (response/response {:cards [(assoc new-card
                                                   :card-title (util/escape-html (:card-title new-card))
                                                   :card-checkboxes (reduce (fn [acc checkbox]
                                                                              (conj acc
                                                                                    (assoc checkbox
                                                                                           :checkbox-title (util/escape-html (:checkbox-title checkbox)))))
                                                                            []
                                                                            (:card-checkboxes new-card)))]}))
                                    (response/response {:cards []})))))


(defmacro get-routes [page-name & url]
  (let [web-url (or (first url)
                    (str "/" page-name))
        page-symbol (symbol (str "page-" page-name))]
    `(concat (list (compojure/GET ~web-url request# (get-page ~page-symbol request#))
                   (compojure/POST (str "/" ~page-name "/ajax") request# (post-ajax ~page-symbol request#)))
             (when (= ~web-url "/")
               (list (compojure/GET (str "/" ~page-name "/ajax") request# (get-ajax ~page-symbol request#)))))))


(def routes (apply compojure/routes
                   (concat (list (compojure/GET "/favicon.ico" request# "")
                                 (compojure/GET "/login" request# (get-page page-login request#)))
                           (get-routes "today" "/")
                           (list (route/resources "/")
                                 (friend/wrap-authorize (apply compojure/routes
                                                               (concat (get-routes "cards")
                                                                       (get-routes "schedule")))
                                                        #{::user})
                                 (friend/logout (compojure/ANY "/logout" request# (response/redirect "/")))                                 
                                 (route/not-found (get-page page-notfound {}))))))


(defn wrap-init-tenant [handler]
  (fn [request]
    (binding [*tenant* (or (:identity (friend/current-authentication))
                           "default")]
      (db/init-tenant! *tenant*)
      (handler request))))


(defn- get-custom-token [request]
  (let [token (or (get-in request [:body :token])
                  (get-in request [:params :__anti-forgery-token]))]
    token))


(defn- simple-credential-fn [{:keys [username password] :as creds}]
  (when (and (= (environ/env :checklist-admin-password) password)
             (= (environ/env :checklist-admin-user) username))
    {:identity username
     :type :simple
     :roles #{::admin}}))


(defn- github-credential-fn
  [token]
  (let [access-token (:access-token token)]
    {:identity (github/github-get-login access-token)
     :access-token access-token
     :type :github
     :roles #{::user}}))


(def ^:dynamic *credential-fn* #'simple-credential-fn)


(def ^:dynamic *github-credential-fn* #'github-credential-fn)


(def app (-> routes
             (json/wrap-json-response)
             (wrap-init-tenant)
             (friend/authenticate {:allow-anon? true
                                   :workflows (concat (when github/github-client-id
                                                        [(github/github-workflow :credential-fn *github-credential-fn*)])
                                                      [(workflows/interactive-form :credential-fn *credential-fn*)])})
             (anti-forgery/wrap-anti-forgery {:read-token get-custom-token})
             (json/wrap-json-body {:keywords? true :bigdecimals? true})
             (defaults/wrap-defaults (-> defaults/site-defaults
                                         (assoc-in [:security :anti-forgery] false)
                                         (assoc-in [:security :ssl-redirect] false)
                                         (assoc-in [:security :hsts] false)
                                         (assoc-in [:session :cookie-attrs :same-site] :lax)
                                         (assoc-in [:params :multipart] false)
                                         (assoc :proxy true)))))


(def reloaded-app (reload/wrap-reload #'app))


(defn init []
  (try
    (timely/start-scheduler)
    (catch IllegalStateException _
      nil)))
