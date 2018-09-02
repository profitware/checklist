(ns checklist.cards-spec
  (:require [clojure.spec.alpha :as s]))


(s/def ::simple-symbol-or-keyword #(re-matches #"^[a-z0-9-]+$" (str (if (keyword? %)
                                                                      (name %)
                                                                      (str %)))))


(s/def ::checkbox-id string?)
(s/def ::checkbox-title string?)
(s/def ::checkbox-disabled boolean?)
(s/def ::checkbox-checked (s/or :bool boolean?
                                :kwd (s/and keyword?
                                            ::simple-symbol-or-keyword)))
(s/def ::checkbox-spec (s/keys :req [::checkbox-id
                                     ::checkbox-title]
                               :opt [::checkbox-disabled
                                     ::checkbox-checked]))


(defmacro check [checkbox-title]
  {:checkbox-id (str "checkbox-" (hash checkbox-title))
   :checkbox-title checkbox-title})


(s/fdef check
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


(s/def ::card-id (s/and string?
                        ::simple-symbol-or-keyword))
(s/def ::card-title string?)
(s/def ::checkboxes-macros (s/or :check (s/cat :smb #{'check}
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
        :args (s/cat :card-id (s/and symbol?
                                     ::simple-symbol-or-keyword)
                     :card-title string?
                     :card-checkboxes (s/* ::checkboxes-macros))
        :ret ::card-spec)


(s/def ::defcard-spec (s/cat :smb #{'defcard}
                             :name (s/and symbol?
                                          ::simple-symbol-or-keyword)
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
