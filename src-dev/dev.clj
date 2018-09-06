(ns dev
  (:require [ring.middleware.reload :as reload]
            [checklist.web :as web]))


(def reloaded-app (reload/wrap-reload #'web/app))
