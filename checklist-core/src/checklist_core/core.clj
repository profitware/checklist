(ns checklist-core.core
  (:require [cljfmt.core]
            [clj-time.core :as dates]
            [timely.core :refer [each-minute
                                 hourly
                                 daily
                                 weekly
                                 monthly
                                 on-days
                                 on-months
                                 on-days-of-week
                                 create-interval
                                 each
                                 every
                                 on
                                 at
                                 in-range
                                 am
                                 pm
                                 hour
                                 minute
                                 day
                                 month
                                 day-of-week
                                 start-time
                                 end-time
                                 time-to-cron
                                 schedule-to-cron
                                 scheduled-item
                                 to-date-obj]])
  (:gen-class))


(def data-string (atom nil))
(def cards-for-query (atom [{:card-title "Some Card"
                             :card-checkboxes [{:checkbox-id "checkbox1"
                                                :checkbox-title "Hello"}]}
                            {:card-title "Well"}
                            {:card-title "This"}
                            {:card-title "escalated"
                             :card-checkboxes [{:checkbox-id "checkbox2"
                                                :checkbox-title "Wow"
                                                :checkbox-checked true}
                                               {:checkbox-id "checkbox3"
                                                :checkbox-title "Test"}]}
                            {:card-title "quickly"}]))


(defn get-default-data-string []
  (str "(def day-start \"0 0 * * *\")" \newline
       "(def day-end \"59 23 * * *\")" \newline
       \newline
       "(def y \"test\")"))


(defn get-data-string [query]
  (cljfmt.core/reformat-string (or @data-string
                                   (get-default-data-string))))



(defn check-schedule [{:keys [schedule work]}]
  (let [schedule-start-time (to-date-obj (:start-time schedule))
        schedule-end-time (to-date-obj (:end-time schedule))
        cron (schedule-to-cron schedule)
        now (dates/now)]
    (and (or (nil? schedule-start-time)
             (dates/before? schedule-start-time now))
         (or (nil? schedule-end-time)
             (dates/before? now schedule-end-time)))))


(defn get-default-cards-for-query []
  )


(defn get-cards-for-query [query]
  (or @cards-for-query
      (get-default-cards-for-query)))


(defn show-card-for-query! [query card]
  (swap! cards-for-query
         (fn [old-cards {card-title :card-title
                         card-checkboxes :card-checkboxes}]
           (if (empty? (filter #(= (:card-title %)
                                   card-title)
                               old-cards))
             (conj old-cards {:card-title card-title
                              :card-checkboxes card-checkboxes})
             (reduce (fn [acc card]
                       (conj acc (if (= (:card-title card)
                                        card-title)
                                   (assoc card
                                          :card-checkboxes card-checkboxes)
                                   card)))
                     []
                     old-cards)))
         card))


(defn hide-card-for-query! [query card]
  (swap! cards-for-query
         (fn [old-cards {card-title :card-title
                         card-checkboxes :card-checkboxes}]
           (reduce (fn [acc card]
                     (if (= (:card-title card)
                            card-title)
                       acc
                       (conj acc card)))
                   []
                   old-cards))
         card))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println (get-default-data-string)))
