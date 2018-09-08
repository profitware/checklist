(ns checklist.web.html
  (:require [cemerick.friend :as friend]
            [hiccup.page :as page]
            [hiccup.util :as util]
            [ring.middleware.anti-forgery :as anti-forgery]
            [ring.util.anti-forgery :as util-anti-forgery]
            [checklist.db :as db]
            [checklist.web.auth :as auth]
            [checklist.web.cards :as cards]
            [checklist.web.editor :as editor]
            [checklist.web.pages :as pages]
            [checklist.web.sidebar :as sidebar]))


(defn get-head [page-name ctx]
  [:head
   [:title (str "Checklist :: " auth/*tenant*)]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
   (page/include-css "/css/patternfly.min.css")
   (page/include-css "/css/patternfly-additions.min.css")
   (page/include-css "/css/codemirror.css")
   (page/include-css "/css/mdn-like.css")
   (page/include-css "/css/main.css")])


(defn get-script [page-name ctx]
  (when-not (= page-name pages/page-login)
    [:script (if-let [content (condp = page-name
                                pages/page-cards (db/get-cards-string auth/*tenant*)
                                pages/page-schedule (db/get-schedule-string auth/*tenant*)
                                nil)]
               (str "initApp($, '" page-name "', '" anti-forgery/*anti-forgery-token* "', `"
                    (clojure.string/replace (util/escape-html content) #"`" "'") "`);")
               (str "initApp($, '" page-name "', '" anti-forgery/*anti-forgery-token* "');"))]))


(defn get-page [page-name ctx]
  (let [auth (:auth ctx)]
    (page/html5 (get-head page-name {})
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
                    (pages/get-menu page-name {})
                    (if (friend/authorized? auth/*editor-roles* auth)
                      (pages/get-menu-logout page-name {})
                      (pages/get-menu-login page-name {}))]]]

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
                     (.contains [pages/page-cards pages/page-schedule] page-name) (editor/get-editor page-name {})
                     (= page-name pages/page-login) (auth/get-login-form page-name ctx)
                     (= page-name pages/page-today) (let [tenant-cards (db/get-cards auth/*tenant*)]
                                                      (cards/get-cards page-name {:cards tenant-cards}))
                     :else (pages/get-not-found page-name {}))
                   (sidebar/get-sidebar page-name ctx)]]

                 (when (.contains [pages/page-cards pages/page-schedule]
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
                 (get-script page-name {})])))
