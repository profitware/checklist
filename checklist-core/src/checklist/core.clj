(ns checklist.core
  (:require [cljfmt.core]
            [clj-time.core :as dates]
            [checklist.cards-spec :as cards-spec]
            [checklist.schedule-spec :as schedule-spec])
  (:gen-class))


(def cards-string (atom nil))

(defn get-default-cards-string []
  (str "(defcard sample-card \"Sample Card\"" \newline
       "  (checkbox \"Hello, world\")" \newline
       "  (auto \"This is a test!\" true))" \newline))


(def schedule-string (atom nil))

(defn get-default-schedule-string []
  (str "(reset sample-card (each-minute))"))


(def cards-for-query (atom (cards-spec/evaluate-expr (get-default-cards-string))))

(def schedule-for-query (atom (schedule-spec/evaluate-expr (get-default-schedule-string))))


(defn get-cards-string [query]
  (or @cards-string
      (get-default-cards-string)))


(defn get-schedule-string [query]
  (or @schedule-string
      (get-default-schedule-string)))


(defn apply-cards-string! [query new-cards-string]
  (let [formatted-string (cljfmt.core/reformat-string new-cards-string)]
    (reset! cards-string formatted-string)
    (reset! cards-for-query (cards-spec/evaluate-expr formatted-string))
    formatted-string))


(defn apply-schedule-string! [query new-schedule-string]
  (let [formatted-string (cljfmt.core/reformat-string new-schedule-string)]
    (reset! schedule-string formatted-string)
    (reset! schedule-for-query (schedule-spec/evaluate-expr formatted-string))
    formatted-string))


(defn get-default-cards-for-query []
  )


(defn get-cards-for-query [query]
  (or @cards-for-query
      (get-default-cards-for-query)))


(defn get-default-schedule-for-query []
  )


(defn get-schedule-for-query [query]
  (or @schedule-for-query
      (get-default-schedule-for-query)))


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
  (println (get-default-cards-string)))
