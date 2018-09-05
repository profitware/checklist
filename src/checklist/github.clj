(ns checklist.github
  (:require [environ.core :as environ]
            [cemerick.friend :as friend]
            [friend-oauth2.workflow :as oauth2]
            [friend-oauth2.util :as oauth2.util]
            [clj-http.client :as client]
            [cheshire.core :as cheshire]))


(def github-client-id (environ/env :checklist-github-client-id))


(def github-login-uri "/login/github")


(defn github-get-repos [access-token]
  (let [url (str "https://api.github.com/user/repos?access_token=" access-token)
        response (client/get url {:accept :json})
        repos (cheshire/parse-string (:body response) true)]
    repos))


(defn github-get-login [access-token]
  (let [url (str "https://api.github.com/user?access_token=" access-token)
        response (client/get url {:accept :json})
        user (cheshire/parse-string (:body response) true)]
    (:login user)))


(def github-client-config
  {:client-id github-client-id
   :client-secret (environ/env :checklist-github-secret)
   :callback {:domain (environ/env :checklist-domain) :path github-login-uri}})


(def github-uri-config
  {:authentication-uri {:url "https://github.com/login/oauth/authorize"
                        :query {:client_id (:client-id github-client-config)
                                :response_type "code"
                                :redirect_uri (oauth2.util/format-config-uri github-client-config)
                                :scope "read:user read:org"}}
   :access-token-uri {:url "https://github.com/login/oauth/access_token"
                      :query {:client_id (:client-id github-client-config)
                              :client_secret (:client-secret github-client-config)
                              :grant_type "authorization_code"
                              :redirect_uri (oauth2.util/format-config-uri github-client-config)}}})


(defn github-workflow [& {:keys [credential-fn] :as form-config}]
  (let [oauth2-workflow (oauth2/workflow {:client-config github-client-config
                                          :uri-config github-uri-config
                                          :access-token-parsefn #'oauth2.util/get-access-token-from-params
                                          :credential-fn credential-fn})]
    (fn [request]
      (when (or (= (:uri request)
                   github-login-uri)
                (= (:type (friend/current-authentication request))
                   :github))
        (oauth2-workflow request)))))
