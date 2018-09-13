(ns checklist.web.pages
  (:require [cemerick.friend :as friend]
            [checklist.web.auth :as auth]))


(defmacro menu [page-name page-title & page-href]
  `[:li {:class (when (= ~'page-name ~page-name)
                  "active")}
    [:a {:href (or ~@page-href
                   (str "/" ~page-name))}
     ~page-title]])


(def page-today "today")
(def page-cards "cards")
(def page-schedule "schedule")
(def page-index "index")
(def page-login "login")
(def page-logout "logout")
(def page-notfound "notfound")
(def page-too-many-requests "too-many-requests")
(def page-profile "profile")


(defn get-menu [page-name ctx]
  (let [auth (:auth ctx)
        next (:next ctx)]
    (if (= next "profile")
      [:ul {:class "nav navbar-nav navbar-primary"}
       (when (friend/authorized? auth/*editor-roles* auth)
         (menu page-profile "Profile"))]
      [:ul {:class "nav navbar-nav navbar-primary"}
       (menu page-today "Today")
       (when (friend/authorized? auth/*editor-roles* auth)
         (menu page-cards "Cards"))
       (when (friend/authorized? auth/*editor-roles* auth)
         (menu page-schedule "Schedule"))])))


(defn get-menu-login [page-name ctx]
  [:ul {:class "nav navbar-nav navbar-right"}
   (menu page-login "Sign in")])


(defn get-menu-logout [page-name ctx]
  [:ul {:class "nav navbar-nav navbar-right"}
   (menu page-logout (str "Logout [" auth/*tenant* "]"))])


(defn get-not-found [page-name ctx]
  [:div {:class "blank-slate-pf"}
   [:h1 "Page not found"]])


(defn get-too-many-requests [page-name ctx]
  [:div {:class "blank-slate-pf"}
   [:h1 "Too many requests"]])
