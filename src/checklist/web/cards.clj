(ns checklist.web.cards
  (:require [hiccup.util :as util]
            [checklist.db :as db]
            [checklist.web.auth :as auth]))


(defn checkbox-checked? [{checkbox-checked :checkbox-checked
                          checkbox-disabled :checkbox-disabled}]
  (or (and (not checkbox-disabled)
           checkbox-checked)
      (and checkbox-disabled
           (if (boolean? checkbox-checked)
             checkbox-checked
             (db/get-context-value auth/*tenant*
                                   checkbox-checked)))))


(defn- get-checkbox [checkbox-id checkbox-title checkbox-checked checkbox-disabled]
  [:div {:class "form-group"}
   [:label {:class "col-sm-9 control-label"
            :for checkbox-id}
    (util/escape-html checkbox-title)]
   [:div {:class "col-sm-3"}
    [:input (into {:type "checkbox"
                   :id checkbox-id
                   :name checkbox-id
                   :class "form-control"}
                  [(when (checkbox-checked? {:checkbox-checked checkbox-checked
                                             :checkbox-disabled checkbox-disabled})
                     [:checked "checked"])
                   (when checkbox-disabled
                     [:disabled "disabled"])])]]])


(defn get-cards-empty-state [page-name ctx]
  (let [cards (:cards ctx)]
    [:div {:class "blank-slate-pf"
           :id "blank"
           :style (when-not (reduce (fn [acc card]
                                      (and acc
                                           (:card-hidden card)))
                                    true
                                    cards)
                    "display: none;")}
     [:div {:class "blank-slate-pf-icon"}
      [:span {:class "pficon pficon pficon-add-circle-o"}]]
     [:h1 "No Cards for Today"]
     [:p "Well, it seems that you don't have anything to do for today. Or you just haven't added anything yet."]
     [:p "In any case, you may perform some actions to check the reasons for this."]
     [:div {:class "blank-slate-pf-main-action"}
      [:a {:class "btn btn-primary btn-lg"
           :href "cards"}
       "Add Cards"]]
     [:div {:class "blank-slate-pf-secondary-action"}
      [:a {:class "btn btn-default"
           :href "schedule"}
       "Check Schedules"]]]))


(defn get-cards [page-name ctx]
  (let [cards (:cards ctx)]
    [:div {:class "col-xs-12 col-sm-12 col-md-9 cards-content"}
     (get-cards-empty-state page-name ctx)
     (for [{card-id :card-id
            card-title :card-title
            card-hidden :card-hidden
            card-highlighted :card-highlighted
            card-checkboxes :card-checkboxes} cards]
       [:div {:class "col-xs-12 col-sm-12 col-md-4"}
        [:div {:class (str "card-pf"
                           (when (reduce (fn [acc checkbox]
                                           (let [checked (get checkbox :checkbox-checked)]
                                             (and acc (or (= checked true)
                                                          (and (keyword? checked)
                                                               (db/get-context-value auth/*tenant*
                                                                                     checked))))))
                                         true
                                         card-checkboxes)
                             " card-disabled")
                           (when card-highlighted
                             " card-highlighted"))
               :style (when card-hidden
                        "display: none;")
               :id card-id}
         [:h2 {:class "card-pf-title"}
          (util/escape-html card-title)
          [:button {:type "button"
                    :class "highlight-card"
                    :aria-label "Highlight"}
           [:span {:class "pficon pficon-thumb-tack-o"}]]
          [:button {:type "button"
                    :class "hide-card close"
                    :aria-label "Close"}
           [:span {:class "pficon pficon-close"}]]]
         [:div {:class "card-pf-body"}
          [:form {:class "form-horizontal"}
           (for [{checkbox-id :checkbox-id
                  checkbox-title :checkbox-title
                  checkbox-checked :checkbox-checked
                  checkbox-disabled :checkbox-disabled} card-checkboxes]
             (get-checkbox checkbox-id checkbox-title checkbox-checked checkbox-disabled))]]]])]))
