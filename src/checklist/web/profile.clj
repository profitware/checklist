(ns checklist.web.profile
  (:require [ring.util.anti-forgery :as util-anti-forgery]))


(defn get-profile-page [page-name ctx]
  [:div {:class "col-xs-12 col-sm-9"}
   (when (:bad-data ctx)
     [:div {:class "alert alert-danger alert-dismissable"}
      [:button {:type "button"
                :class "close"
                :aria-label "Close"}
       [:span {:class "pficon pficon-close"}]]
      [:span {:class "pficon pficon-error-circle-o"}]
      [:strong "Error!"]
      " Something went wrong!"])

   [:form {:class "form-horizontal"
           :accept-charset "UTF-8"
           :method "POST"}
    (util-anti-forgery/anti-forgery-field)
    [:div {:class "form-group"}
     [:label {:class "col-sm-3 control-label"
              :for "some-field"}
      "Some field"]
     [:div {:class "col-sm-6"}
      [:input {:type "text"
               :id "some-field"
               :name "some-field"
               :class "form-control"
               :autofocus "autofocus"
               :autocapitalize "off"
               :autocorrect "off"
               :autocomplete "off"}]]]
    [:div {:style "padding-top: 10px; padding-bottom: 10px;"
           :class "row"}
     [:div {:class "col-sm-6 col-sm-offset-3"}
      [:span
       [:button {:type "submit"
                 :id "submit"
                 :class "btn btn-primary"
                 :data-disable-with "Submitting..."}
        "Submit"]
       " "]]]]])
