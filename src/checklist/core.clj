(ns checklist.core
  (:require [ring.adapter.jetty :as jetty]
            [timely.core :as timely]
            [checklist.web :as web])
  (:gen-class))


(defn -main []
  (try
    (timely/start-scheduler)
    (catch IllegalStateException _
      nil))

  (jetty/run-jetty #'web/app
                   {:port 5000}))
