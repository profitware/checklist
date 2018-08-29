(ns checklist.schedule
  (:require [cljfmt.core]
            [clj-time.core :as dates]
            [checklist.schedule-spec :as schedule-spec])
  (:gen-class))


(def schedule-string (atom nil))


(defn get-default-schedule-string []
  (str "(reset sample-card (each-minute))"))


(def schedule-for-query (atom (schedule-spec/evaluate-expr (get-default-schedule-string))))


(defn get-schedule-string [query]
  (or @schedule-string
      (get-default-schedule-string)))


(defn apply-schedule-string! [query new-schedule-string]
  (let [formatted-string (cljfmt.core/reformat-string new-schedule-string)]
    (reset! schedule-string formatted-string)
    (reset! schedule-for-query (schedule-spec/evaluate-expr formatted-string))
    formatted-string))


(defn get-default-schedule-for-query []
  )


(defn get-schedule-for-query [query]
  (or @schedule-for-query
      (get-default-schedule-for-query)))
