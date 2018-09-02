(ns checklist.core
  (:require [ring.adapter.jetty :as jetty]
            [checklist.web :as web])
  (:gen-class))


(defn -main []
  (jetty/run-jetty #'web/app
                   {:port 3000}))
