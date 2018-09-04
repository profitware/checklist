(ns checklist.schedule-spec
  (:require [clojure.spec.alpha :as s]
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
                                 to-date-obj]]))


(s/def ::simple-symbol-or-keyword #(re-matches #"^[a-z0-9-]+$" (str (if (keyword? %)
                                                                      (name %)
                                                                      (str %)))))


(s/def ::schedule-id string?)
(s/def ::schedule-type (s/and keyword?
                              ::simple-symbol-or-keyword))
(s/def ::schedule-card (s/and keyword?
                              ::simple-symbol-or-keyword))
(s/def ::schedule-context (s/and keyword?
                                 ::simple-symbol-or-keyword))
(s/def ::schedule (s/keys :req [::minute
                                ::hour
                                ::day
                                ::month
                                ::day-of-week]))
(s/def ::schedule-spec (s/keys :req [::schedule-id
                                     ::schedule-type
                                     ::schedule]
                               :opt [::schedule-card
                                     ::schedule-context]))
(s/def ::schedule-schedule-spec (s/or :simple (s/cat :smb (->> ["each-minute"
                                                                "hourly"
                                                                "daily"
                                                                "weekly"
                                                                "monthly"]
                                                               (map symbol)
                                                               set))
                                      :on-days (s/cat :smb #{'on-days}
                                                      :vec (s/coll-of (s/and number?
                                                                             #(and (> % 0)
                                                                                   (< % 32)))))
                                      :on-days-of-week (s/cat :smb #{'on-days-of-week}
                                                              :vec (s/coll-of #{:mon :tue :wed :thu :fri :sat :sun}))
                                      :on-months  (s/cat :smb #{'on-months}
                                                         :vec (s/coll-of #{:jan :feb :mar :apr :may :jun
                                                                           :jul :aug :sep :oct :nov :dec}))
                                      :every (s/cat :smb #{'every}
                                                    :int (s/and number?
                                                                #(< % 100000000))
                                                    :every #{:minute :minutes
                                                             :hour :hours
                                                             :day :days
                                                             :month :months
                                                             :day-of-week :days-of-week})))


(defmacro hide [schedule-card schedule]
  (let [schedule-card-str (str schedule-card)]
    {:schedule-id (str "schedule-" (hash [:hide schedule-card-str schedule]))
     :schedule-type :hide
     :schedule-card schedule-card-str
     :schedule schedule}))


(s/fdef hide
        :args (s/cat :schedule-card (s/and symbol?
                                           ::simple-symbol-or-keyword)
                     :schedule ::schedule-schedule-spec)
        :ret ::schedule-spec)


(defmacro show [schedule-card schedule]
  (let [schedule-card-str (str schedule-card)]
    {:schedule-id (str "schedule-" (hash [:show schedule-card-str schedule]))
     :schedule-type :show
     :schedule-card schedule-card-str
     :schedule schedule}))


(s/fdef show
        :args (s/cat :schedule-card (s/and symbol?
                                           ::simple-symbol-or-keyword)
                     :schedule ::schedule-schedule-spec)
        :ret ::schedule-spec)


(defmacro highlight [schedule-card schedule]
  (let [schedule-card-str (str schedule-card)]
    {:schedule-id (str "schedule-" (hash [:highlight schedule-card-str schedule]))
     :schedule-type :highlight
     :schedule-card schedule-card-str
     :schedule schedule}))


(s/fdef highlight
        :args (s/cat :schedule-card (s/and symbol?
                                           ::simple-symbol-or-keyword)
                     :schedule ::schedule-schedule-spec)
        :ret ::schedule-spec)


(defmacro unhighlight [schedule-card schedule]
  (let [schedule-card-str (str schedule-card)]
    {:schedule-id (str "schedule-" (hash [:unhighlight schedule-card-str schedule]))
     :schedule-type :unhighlight
     :schedule-card schedule-card-str
     :schedule schedule}))


(s/fdef unhighlight
        :args (s/cat :schedule-card (s/and symbol?
                                           ::simple-symbol-or-keyword)
                     :schedule ::schedule-schedule-spec)
        :ret ::schedule-spec)


(defmacro reset [schedule-card schedule]
  (let [schedule-card-str (str schedule-card)]
    {:schedule-id (str "schedule-" (hash [:reset schedule-card-str schedule]))
     :schedule-type :reset
     :schedule-card schedule-card-str
     :schedule schedule}))


(s/fdef reset
        :args (s/cat :schedule-card (s/and symbol?
                                           ::simple-symbol-or-keyword)
                     :schedule ::schedule-schedule-spec)
        :ret ::schedule-spec)


(defmacro check [schedule-context schedule]
  {:schedule-id (str "schedule-" (hash [:check schedule-context schedule]))
   :schedule-type :check
   :schedule-context schedule-context
   :schedule schedule})


(s/fdef check
        :args (s/cat :schedule-context (s/and keyword?
                                              ::simple-symbol-or-keyword)
                     :schedule ::schedule-schedule-spec)
        :ret ::schedule-spec)


(defmacro uncheck [schedule-context schedule]
  {:schedule-id (str "schedule-" (hash [:uncheck schedule-context schedule]))
   :schedule-type :uncheck
   :schedule-context schedule-context
   :schedule schedule})


(s/fdef uncheck
        :args (s/cat :schedule-context (s/and keyword?
                                              ::simple-symbol-or-keyword)
                     :schedule ::schedule-schedule-spec)
        :ret ::schedule-spec)


(defmacro toggle [schedule-context schedule]
  {:schedule-id (str "schedule-" (hash [:toggle schedule-context schedule]))
   :schedule-type :toggle
   :schedule-context schedule-context
   :schedule schedule})


(s/fdef toggle
        :args (s/cat :schedule-context (s/and keyword?
                                              ::simple-symbol-or-keyword)
                     :schedule ::schedule-schedule-spec)
        :ret ::schedule-spec)


(s/def ::schedule-input-spec (s/or :crd (s/cat :cmd (->> ["hide"
                                                          "show"
                                                          "highlight"
                                                          "unhighlight"
                                                          "reset"]
                                                         (map symbol)
                                                         set)
                                               :smb (s/and symbol?
                                                           ::simple-symbol-or-keyword)
                                               :schedule ::schedule-schedule-spec)
                                   :ctx (s/cat :cmd (->> ["check"
                                                          "uncheck"
                                                          "toggle"]
                                                         (map symbol)
                                                         set)
                                               :kwd (s/and keyword?
                                                           ::simple-symbol-or-keyword)
                                               :schedule ::schedule-schedule-spec)))


(s/def ::evaluation (s/coll-of ::schedule-input-spec
                               :kind vector?
                               :distinct true))


(defn evaluate-expr [evaluation-string]
  (try
    (let [expr (read-string (str "[" evaluation-string "]"))]
      (when (s/valid? ::evaluation expr)
        (binding [*ns* (find-ns 'checklist.schedule-spec)]
          (eval expr))))
    (catch RuntimeException e
      nil)))
