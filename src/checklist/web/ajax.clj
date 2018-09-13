(ns checklist.web.ajax
  (:require [hiccup.util :as util]
            [ring.util.response :as response]
            [checklist.db :as db]
            [checklist.web.auth :as auth]
            [checklist.web.cards :as cards]
            [checklist.web.html :as html]
            [checklist.web.pages :as pages]))


(defn get-ajax [page-name ctx]
  (cond
    (= page-name pages/page-today) (let [escape-checkbox-titles (fn [acc checkbox]
                                                                  (conj acc
                                                                        (assoc checkbox
                                                                               :checkbox-title (util/escape-html (:checkbox-title checkbox))
                                                                               :checkbox-checked (cards/checkbox-checked? checkbox))))]
                                     (response/response {:cards (map #(assoc %
                                                                             :card-title (util/escape-html (:card-title %))
                                                                             :card-checkboxes (reduce escape-checkbox-titles
                                                                                                      []
                                                                                                      (:card-checkboxes %)))
                                                                     (db/get-cards auth/*tenant*))}))
    (= page-name pages/page-index) (response/response (merge {:auth auth/*tenant*}
                                                             (when-let [next-page (:next ctx)]
                                                               {:next next-page})))
    :else (response/not-found (html/get-page pages/page-notfound ctx))))


(defn post-ajax [page-name ctx]
  (let [input-json (:body ctx)]
    (cond
      (= page-name pages/page-cards) (let [cards-code (:cards-code input-json)]
                                       (if-let [cards-expr (db/expression-of-type :cards cards-code)]
                                         (let [formatted-string (db/upsert-cards-string! auth/*tenant* cards-code)]
                                           (response/response {:cards-code formatted-string}))
                                         (response/response {:error "Cannot evaluate expression"})))
      (= page-name pages/page-schedule) (let [schedule-code (:schedule-code input-json)]
                                          (if-let [schedule-expr (db/expression-of-type :schedule schedule-code)]
                                            (let [formatted-string (db/upsert-schedule-string! auth/*tenant* schedule-code)]
                                              (response/response {:schedule-code formatted-string}))
                                            (response/response {:error "Cannot evaluate expression"})))
      (= page-name pages/page-today) (let [card input-json
                                           {checked-ids :checked
                                            hide-card :hide
                                            highlight-card :highlight} card
                                           old-cards (db/get-cards auth/*tenant*)
                                           old-card (first (filter #(= (:card-id %)
                                                                       (:card-id card))
                                                                   old-cards))
                                           change-checkboxes (fn [acc old-checkbox]
                                                               (conj acc (if (:checkbox-disabled old-checkbox)
                                                                           old-checkbox
                                                                           (assoc old-checkbox
                                                                                  :checkbox-checked (.contains checked-ids
                                                                                                               (:checkbox-id old-checkbox))))))
                                           escape-checkbox-titles (fn [acc checkbox]
                                                                    (conj acc
                                                                          (assoc checkbox
                                                                                 :checkbox-title (util/escape-html (:checkbox-title checkbox)))))]
                                       (if old-card
                                         (let [new-card (assoc old-card
                                                               :card-hidden (boolean hide-card)
                                                               :card-highlighted (boolean highlight-card)
                                                               :card-checkboxes (reduce change-checkboxes
                                                                                        []
                                                                                        (:card-checkboxes old-card)))]
                                           (db/upsert-card! auth/*tenant* new-card)
                                           (response/response {:cards [(assoc new-card
                                                                              :card-title (util/escape-html (:card-title new-card))
                                                                              :card-checkboxes (reduce escape-checkbox-titles
                                                                                                       []
                                                                                                       (:card-checkboxes new-card)))]}))
                                         (response/response {:cards []})))
      :else (response/response {}))))
