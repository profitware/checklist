(ns checklist.web.editor
  (:require [checklist.web.pages :as pages]))


(defn get-editor [page-name ctx]
  [:div {:class "col-xs-12 col-sm-9"}
   (when (.contains [pages/page-cards pages/page-schedule]
                    page-name)
     [:div {:class "alert alert-danger alert-dismissable"
            :style "display: none;"}
      [:button {:type "button"
                :class "close"
                :aria-label "Close"}
       [:span {:class "pficon pficon-close"}]]
      [:span {:class "pficon pficon-error-circle-o"}]
      [:strong "Error!"]
      " Your syntax is wrong!"])

   (when (.contains [pages/page-cards pages/page-schedule]
                    page-name)
     [:div {:class "alert alert-success alert-dismissable"
            :style "display: none;"}
      [:button {:type "button"
                :class "close"
                :aria-label "Close"}
       [:span {:class "pficon pficon-close"}]]
      [:span {:class "pficon pficon-ok"}]
      [:strong "Success!"]
      (condp = page-name
        pages/page-cards " Your cards are updated now!"
        pages/page-schedule " Your schedule is updated now!")])

   [:div {:id "codemirror"}]])
