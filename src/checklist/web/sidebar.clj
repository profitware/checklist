(ns checklist.web.sidebar
  (:require [hiccup.util :as util]
            [checklist.db :as db]
            [checklist.web.auth :as auth]
            [checklist.web.pages :as pages]))


(defn get-sidebar [page-name ctx]
  [:div {:id "sidebar"
         :class "col-xs-6 col-sm-3 sidebar-offcanvas"}
   (when (= page-name
            pages/page-today)
     [:div {:class "toast-pf alert alert-warning alert-dismissable"
            :style "display: none;"}
      [:button {:type "button"
                :class "close"
                :data-dismiss "alert"
                :aria-hidden "true"}
       [:span {:class "pficon pficon-close"}]]
      [:div {:class "pull-right toast-pf-action"}
       [:a {:href "."}
        "Reload Page"]]
      [:span {:class "pficon pficon-warning-triangle-o"}]
      "Error connecting to server."])
   (when (.contains [pages/page-cards pages/page-schedule]
                    page-name)
     [:p (str "Press the button below to run or re-run the job. "
              "Once the job is finished, the contents on the left would be updated.")])
   (when (= page-name
            pages/page-cards)
     [:button {:class "btn btn-primary action-button"
               :type "button"}
      "Update Cards"])
   (when (= page-name
            pages/page-schedule)
     [:button {:class "btn btn-primary action-button"
               :type "button"}
      "Update Schedule"])
   [:p {:class "loading-contents"
        :style "display: none;"}
    [:span {:class "spinner spinner-xs spinner-inline"}]
    "\nWait while job is running"]
   (when (= page-name
            pages/page-today)
     (let [tenant-cards (db/get-cards auth/*tenant*)
           hidden-tenant-cards (filter :card-hidden tenant-cards)]
       (when-not (empty? tenant-cards)
         [:div
          [:p {:class "hidden-message"
               :style (when (empty? hidden-tenant-cards)
                        "display: none;")}
           (str "The following cards are hidden:")]
          [:div
           (for [card tenant-cards]
             [:span
              [:button {:class (str "btn btn-default show-card")
                        :data-card-id (:card-id card)
                        :style (when-not (:card-hidden card)
                                 "display: none;")
                        :type "button"}
               (util/escape-html (:card-title card))]
              " "])]])))])
