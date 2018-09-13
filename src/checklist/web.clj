(ns checklist.web
  (:require [compojure.core :as compojure]
            [compojure.route :as route]
            [environ.core :as environ]
            [ring.middleware.anti-forgery :as anti-forgery]
            [ring.middleware.json :as json]
            [ring.middleware.defaults :as defaults]
            [ring.middleware.session.cookie :as cookie]
            [ring.middleware.ratelimit :as ratelimit]
            [ring.middleware.ratelimit.limits :as limits]
            [ring.util.response :as response]
            [cemerick.friend :as friend]
            [checklist.util :as util]
            [checklist.web.ajax :as ajax]
            [checklist.web.auth :as auth]
            [checklist.web.pages :as pages]
            [checklist.web.html :as html])
  (:gen-class))


(defn- get-routes [page-name & [url]]
  (let [web-url (or url
                    (str "/" page-name))
        page-symbol (eval (symbol (str "checklist.web.pages/page-" page-name)))]
    (concat (list (compojure/GET web-url
                                 request#
                                 (html/get-page page-symbol
                                                {:auth (identity request#)
                                                 :next (:next request#)}))
                  (compojure/POST (str "/" page-name "/ajax")
                                  request#
                                  (ajax/post-ajax page-symbol
                                                  {:body (:body request#)})))
            (when (.contains ["/" "/today"] web-url)
              (list (compojure/GET (str "/" page-name "/ajax")
                                   request#
                                   (ajax/get-ajax page-symbol
                                                  {:body (:body request#)
                                                   :next (:next request#)})))))))


(defn- authorized-routes [routes not-found-action]
  (concat (get-routes "index" "/")
          (list (route/resources "/")
                (friend/wrap-authorize (apply compojure/routes
                                              routes)
                                       auth/*editor-roles*)
                (friend/logout (compojure/ANY "/logout" request# (response/redirect "/")))
                (route/not-found not-found-action))))


(defn- get-final-routes []
  (apply compojure/routes
         (concat (list (compojure/GET "/login"
                                      request#
                                      (html/get-page pages/page-login
                                                     {:auth (identity request#)
                                                      :login-failed (get-in request#
                                                                            [:params :login_failed])})))
                 (authorized-routes (concat (get-routes "today")
                                            (get-routes "cards")
                                            (get-routes "schedule"))
                                    (html/get-page pages/page-notfound {})))))


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


(defn- too-many-requests-handler [request]
  (if (.endsWith (:uri request) "/ajax")
    {:status 429
     :headers {"Content-Type" "application/json"}
     :body "{\"error\": \"Too many requests\"}"}
    {:status 429
     :headers {"Content-Type" "text/html"}
     :body (html/get-page pages/page-too-many-requests {})}))


(defn- wrap-limit-cookie [limit cookie]
  (merge limit
         {:key-prefix (str (:key-prefix limit) "C" cookie)
          :filter #(and (get-in % [:cookies cookie]) ((:filter limit) %))
          :getter #(str (get-in % [:cookies cookie]) ((:getter limit) %))}))


(defn- cookie-limit [cookie n] (-> n limits/limit (wrap-limit-cookie cookie)))


(defn- get-env-int [environ-key default]
  (if-let [value-string (re-find #"\d+" (environ/env environ-key (str default)))]
    (Integer. value-string)
    default))


(def ^:dynamic *limit-cookie-name* "__cfduid")
(def ^:dynamic *limit-ip* (get-env-int :checklist-ratelimit-ip 200))
(def ^:dynamic *limit-anonymous* (get-env-int :checklist-ratelimit-anonymous 200))
(def ^:dynamic *limit-user* (get-env-int :checklist-ratelimit-user 600))


(def ^:dynamic *unskippable-pages-function* (fn [request]
                                              nil))


(defn- wrap-unskippable-page [handler {:keys [get-current-pages default-route]}]
  (fn [request]
    (if-let [route-names (and (friend/authorized? auth/*editor-roles* (friend/identity request))
                              get-current-pages
                              (get-current-pages request))]
      (let [routes (reduce (fn [acc route-name]
                             (concat acc
                                     (if (sequential? route-name)
                                       (get-routes (first route-name) (second route-name))
                                       (get-routes route-name))))
                           []
                           route-names)
            default-route (or default-route
                              (first route-names))]
        (let [uri (:uri request)
              routes-fn (-> (apply compojure/routes (authorized-routes routes
                                                                       (response/redirect "/")))
                            json/wrap-json-response
                            auth/wrap-init-tenant)
              response (apply routes-fn
                              [(assoc request
                                      :next default-route)])]
          (if (= (:status response) 404)
            (response/redirect "/")
            response)))
      (handler request))))


(defn get-app []
  (-> (get-final-routes)
      (json/wrap-json-response)
      (auth/wrap-init-tenant)
      (wrap-unskippable-page {:get-current-pages *unskippable-pages-function*})
      (ratelimit/wrap-ratelimit {:limits [(-> *limit-user*
                                              limits/limit
                                              limits/wrap-limit-user
                                              (wrap-limit-cookie *limit-cookie-name*)
                                              limits/wrap-limit-ip)
                                          (-> *limit-anonymous*
                                              limits/limit
                                              (wrap-limit-cookie *limit-cookie-name*)
                                              limits/wrap-limit-ip)
                                          (ratelimit/ip-limit *limit-ip*)]
                                 :err-handler too-many-requests-handler})
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


(def app (get-app))
