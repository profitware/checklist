(ns checklist.web
  (:require [compojure.core :as compojure]
            [compojure.route :as route]
            [environ.core :as environ]
            [ring.middleware.anti-forgery :as anti-forgery]
            [ring.middleware.json :as json]
            [ring.middleware.defaults :as defaults]
            [ring.middleware.session.cookie :as cookie]
            [ring.util.response :as response]
            [cemerick.friend :as friend]
            [checklist.util :as util]
            [checklist.web.ajax :as ajax]
            [checklist.web.auth :as auth]
            [checklist.web.pages :as pages]
            [checklist.web.html :as html])
  (:gen-class))


(defmacro get-routes [page-name & url]
  (let [web-url (or (first url)
                    (str "/" page-name))
        page-symbol (symbol (str "pages/page-" page-name))]
    `(concat (list (compojure/GET ~web-url
                                  request#
                                  (html/get-page ~page-symbol
                                                 {:auth (identity request#)}))
                   (compojure/POST (str "/" ~page-name "/ajax")
                                   request#
                                   (ajax/post-ajax ~page-symbol
                                                   {:body (:body request#)})))
             (when (= ~web-url "/")
               (list (compojure/GET (str "/" ~page-name "/ajax")
                                    request#
                                    (ajax/get-ajax ~page-symbol
                                                   {:body (:body request#)})))))))


(def routes (apply compojure/routes
                   (concat (list (compojure/GET "/favicon.ico" request# "")
                                 (compojure/GET "/login"
                                                request#
                                                (html/get-page pages/page-login
                                                               {:auth (identity request#)
                                                                :login-failed (get-in request#
                                                                                      [:params :login_failed])})))
                           (get-routes "today" "/")
                           (list (route/resources "/")
                                 (friend/wrap-authorize (apply compojure/routes
                                                               (concat (get-routes "cards")
                                                                       (get-routes "schedule")))
                                                        auth/*editor-roles*)
                                 (friend/logout (compojure/ANY "/logout" request# (response/redirect "/")))
                                 (route/not-found (html/get-page pages/page-notfound {}))))))


(defn- get-custom-token [request]
  (let [token (or (get-in request [:body :token])
                  (get-in request [:params :__anti-forgery-token]))]
    token))


(defn- get-cookie-store-key-options []
  (when-let [session-key (environ/env :checklist-session-key)]
    (let [options (if (= (count session-key) 32)
                    {:key (-> session-key
                              util/unhexify
                              byte-array)}
                    {:key session-key})]
      options)))


(def app (-> routes
             (json/wrap-json-response)
             (auth/wrap-init-tenant)
             (friend/authenticate {:allow-anon? true
                                   :workflows (auth/get-workflows)})
             (anti-forgery/wrap-anti-forgery {:read-token get-custom-token})
             (json/wrap-json-body {:keywords? true :bigdecimals? true})
             (defaults/wrap-defaults (-> defaults/site-defaults
                                         (assoc-in [:security :anti-forgery] false)
                                         (assoc-in [:security :ssl-redirect] false)
                                         (assoc-in [:security :hsts] false)
                                         (assoc-in [:session :cookie-attrs :same-site] :lax)
                                         (assoc-in [:session :store]
                                                   (cookie/cookie-store (get-cookie-store-key-options)))
                                         (assoc-in [:params :multipart] false)
                                         (assoc :proxy true)))))
