(ns checklist.web.index
  (:require [checklist.web.github :as github]))


(defn get-index-page [page-name ctx]
  [:div {:class "col-xs-12 col-sm-9 hidden-index"
         :style "display: none;"}
   [:div {:class "jumbotron"}
    [:h1 "Welcome to Checklist!"]
    [:div {:style "padding-top: 3em;"}]
    [:p (str "The application is intended to be used as To Do lists "
             "with the ability to bind some checkboxes to automatic events. "
             "It is written in Clojure and uses LISP DSL to describe cards with checkboxes "
             "and schedules to show, hide, reset and highlight them.")]
    [:div {:style "padding-top: 3em;"}]
    (if github/github-client-id
      [:a {:class "btn btn-primary"
           :href "/login/github"}
       "Sign in using GitHub"]
      [:a {:class "btn btn-primary"
           :href "/login"}
       "Sign in"])]])
