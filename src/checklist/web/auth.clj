(ns checklist.web.auth
  (:require [cemerick.friend.workflows :as workflows]
            [environ.core :as environ]
            [cemerick.friend :as friend]
            [ring.util.anti-forgery :as util-anti-forgery]
            [checklist.db :as db]
            [checklist.web.github :as github]))


(def ^:dynamic *tenant* "default")


(derive ::admin ::user)


(def ^:dynamic *admin-roles* #{::admin})
(def ^:dynamic *editor-roles* #{::user})


(defn wrap-init-tenant [handler]
  (fn [request]
    (binding [*tenant* (or (:identity (friend/current-authentication))
                           "default")]
      (db/init-tenant! *tenant*)
      (handler request))))


(defn- simple-credential-fn [{:keys [username password] :as creds}]
  (when (and (= (environ/env :checklist-admin-password) password)
             (= (environ/env :checklist-admin-user) username))
    {:identity username
     :type :simple
     :roles *admin-roles*}))


(defn- github-credential-fn
  [token]
  (let [access-token (:access-token token)]
    {:identity (github/github-get-login access-token)
     :access-token access-token
     :type :github
     :roles *editor-roles*}))


(def ^:dynamic *credential-fn* #'simple-credential-fn)


(def ^:dynamic *github-credential-fn* #'github-credential-fn)


(defn get-workflows []
  (concat (when github/github-client-id
            [(github/github-workflow :credential-fn *github-credential-fn*)])
          [(workflows/interactive-form :credential-fn *credential-fn*)]))


(defn get-login-form [page-name ctx]
  [:div {:class "col-xs-12 col-sm-9"}
   (when (:login-failed ctx)
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
