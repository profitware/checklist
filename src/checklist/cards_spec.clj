(ns checklist.cards-spec
  (:require [clojure.spec.alpha :as s]))


(s/def ::checkbox-id string?)
(s/def ::checkbox-title string?)
(s/def ::checkbox-disabled boolean?)
(s/def ::checkbox-checked (s/or :bool boolean?
                                :kwd keyword?))
(s/def ::checkbox-spec (s/keys :req [::checkbox-id
                                     ::checkbox-title]
                               :opt [::checkbox-disabled
                                     ::checkbox-checked]))


(defmacro checkbox [checkbox-title]
  {:checkbox-id (str "checkbox-" (hash checkbox-title))
   :checkbox-title checkbox-title})


(s/fdef checkbox
        :args (s/cat :checkbox-title string?)
        :ret ::checkbox-spec)


(defmacro auto [checkbox-title checkbox-checked]
  {:checkbox-id (str "checkbox-" (hash checkbox-title))
   :checkbox-title checkbox-title
   :checkbox-disabled true
   :checkbox-checked checkbox-checked})


(s/fdef auto
        :args (s/cat :checkbox-title string?
                     :checkbox-checked (s/or :kwd keyword?
                                             :bool boolean?))
        :ret ::checkbox-spec)


(s/def ::card-id string?)
(s/def ::card-title string?)
(s/def ::checkboxes-macros (s/or :checkbox (s/cat :smb #{'checkbox}
                                                  :str string?)
                                 :auto (s/cat :smb #{'auto}
                                              :str string?
                                              :kwd ::checkbox-checked)))
(s/def ::card-spec (s/keys :req [::card-id
                                 ::card-title
                                 ::card-checkboxes]))

(defmacro defcard [card-id card-title & checkboxes]
  {:card-id (str card-id)
   :card-title card-title
   :card-checkboxes (distinct `(list ~@checkboxes))})


(s/fdef defcard
        :args (s/cat :card-id symbol?
                     :card-title string?
                     :card-checkboxes (s/* ::checkboxes-macros))
        :ret ::card-spec)


(s/def ::defcard-spec (s/cat :smb #{'defcard}
                             :name symbol?
                             :str string?
                             :checkboxes (s/* ::checkboxes-macros)))


(s/def ::evaluation (s/coll-of ::defcard-spec
                               :kind vector?
                               :distinct true))


(defn evaluate-expr [evaluation-string]
  (try
    (let [expr (read-string (str "[" evaluation-string "]"))]
      (when (s/valid? ::evaluation expr)
        (binding [*ns* (find-ns 'checklist.cards-spec)]
          (eval expr))))
    (catch RuntimeException e
      nil)))