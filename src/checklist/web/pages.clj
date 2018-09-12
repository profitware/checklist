(ns checklist.web.pages
  (:require [checklist.web.auth :as auth]))


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


(defn get-menu [page-name ctx]
  [:ul {:class "nav navbar-nav navbar-primary"}
   (menu page-today "Today")
   (menu page-cards "Cards")
   (menu page-schedule "Schedule")])


(defn get-menu-login [page-name ctx]
  [:ul {:class "nav navbar-nav navbar-right"}
   (menu page-login "Sign in")])


(defn get-menu-logout [page-name ctx]
  [:ul {:class "nav navbar-nav navbar-right"}
   (menu page-logout (str "Logout [" auth/*tenant* "]"))])


(defn get-not-found [page-name ctx]
  [:div {:class "blank-slate-pf"}
   [:h1 "Page not found"]])
