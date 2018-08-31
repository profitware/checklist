(ns checklist.db
  (:require [datascript.core :as datascript]
            [cljfmt.core]
            [checklist.cards-spec :as cards-spec]
            [checklist.schedule-spec :as schedule-spec])
  (:gen-class))


(defn expression-of-type [type expression]
  (case type
    :cards (cards-spec/evaluate-expr expression)
    :schedule (schedule-spec/evaluate-expr expression)))


(def checklist-schema {:card/id {:db/cardinality :db.cardinality/one
                                 :db/index true
                                 :db/unique :db.unique/identity}
                       :card/symbol {:db/cardinality :db.cardinality/one}
                       :card/title {:db/cardinality :db.cardinality/one}
                       :card/tenant {:db/cardinality :db.cardinality/one}
                       :card/deleted {:db/cardinality :db.cardinality/one}
                       :cards-string/id {:db/cardinality :db.cardinality/one
                                        :db/index true
                                        :db/unique :db.unique/identity}
                       :cards-string/body {:db/cardinality :db.cardinality/one}
                       :cards-string/tenant {:db/cardinality :db.cardinality/one}
                       :checkbox/id {:db/cardinality :db.cardinality/one
                                     :db/index true
                                     :db/unique :db.unique/identity}
                       :checkbox/card-id {:db/cardinality :db.cardinality/one
                                          :db/valueType :db.type/ref}
                       :checkbox/id-str {:db/cardinality :db.cardinality/one}
                       :checkbox/deleted {:db/cardinality :db.cardinality/one}
                       :checkbox/order {:db/cardinality :db.cardinality/one}
                       :checkbox/title {:db/cardinality :db.cardinality/one}
                       :checkbox/disabled {:db/cardinality :db.cardinality/one}
                       :checkbox/checked {:db/cardinality :db.cardinality/one}
                       :checkbox/tenant {:db/cardinality :db.cardinality/one}
                       :schedule-string/id {:db/cardinality :db.cardinality/one
                                            :db/index true
                                            :db/unique :db.unique/identity}
                       :schedule-string/body {:db/cardinality :db.cardinality/one}
                       :schedule-string/tenant {:db/cardinality :db.cardinality/one}
                       :schedule/id {:db/cardinality :db.cardinality/one
                                     :db/index true
                                     :db/unique :db.unique/identity}
                       :schedule/id-str {:db/cardinality :db.cardinality/one}
                       :schedule/deleted {:db/cardinality :db.cardinality/one}
                       :schedule/string-id {:db/cardinality :db.cardinality/one
                                            :db/valueType :db.type/ref}
                       :schedule/schedule-type {:db/cardinality :db.cardinality/one}
                       :schedule/schedule-card {:db/cardinality :db.cardinality/one}
                       :schedule/schedule-context {:db/cardinality :db.cardinality/one}
                       :schedule/schedule-schedule {:db/cardinality :db.cardinality/one}
                       :schedule/order {:db/cardinality :db.cardinality/one}
                       :schedule/tenant {:db/cardinality :db.cardinality/one}
                       :context/id  {:db/cardinality :db.cardinality/one
                                     :db/index true
                                     :db/unique :db.unique/identity}
                       :context/id-str {:db/cardinality :db.cardinality/one}
                       :context/value {:db/cardinality :db.cardinality/one}
                       :context/tenant {:db/cardinality :db.cardinality/one}})


(def checklist-conn (datascript/create-conn checklist-schema))


(defn- get-card-document-id [tenant card]
  (let [{card-id-symbol :card-id} card]
    (ffirst (datascript/q '[:find ?card
                            :in $ [?card-id-symbol ?card-tenant]
                            :where
                            [?card :card/id-symbol ?card-id-symbol]
                            [?card :card/deleted false]
                            [?card :card/tenant ?card-tenant]]
                          @checklist-conn
                          [card-id-symbol tenant]))))


(defn- get-old-checkboxes [tenant card-id]
  (datascript/pull-many @checklist-conn
                        '[*]
                        (map second
                             (datascript/q '[:find ?checkbox-title (max ?checkbox)
                                             :in $ [?card-id ?checkbox-tenant]
                                             :where
                                             [?checkbox :checkbox/card-id ?card-id]
                                             [?checkbox :checkbox/title ?checkbox-title]
                                             [?checkbox :checkbox/deleted false]
                                             [?checkbox :checkbox/tenant ?checkbox-tenant]]
                                           @checklist-conn
                                           [card-id tenant]))))


(def ^:dynamic *drop-old-checkbox-state* true)


(defn upsert-card! [tenant card]
  (let [{card-id-symbol :card-id
         card-title :card-title
         card-checkboxes :card-checkboxes
         card-deleted :card-deleted} card
        document-id (or (get-card-document-id tenant card) -1)
        old-checkboxes (get-old-checkboxes tenant document-id)
        checkboxes-to-delete (map #(assoc % :checkbox/deleted true)
                                  (filter #(not (.contains (map :checkbox-title card-checkboxes)
                                                           (:checkbox/title %)))
                                          old-checkboxes))
        card-checkboxes-to-upsert (map #(assoc % :checkbox-checked (let [checkbox-checked (:checkbox-checked %)]
                                                                     (and (or (nil? checkbox-checked)
                                                                              checkbox-checked)
                                                                          (or (and (:checkbox-disabled %)
                                                                                   (if (keyword? checkbox-checked)
                                                                                     checkbox-checked
                                                                                     true))
                                                                              (or *drop-old-checkbox-state*
                                                                                  (let [checkbox-title (:checkbox-title %)]
                                                                                    (reduce (fn [acc old-checkbox]
                                                                                              (or acc
                                                                                                  (and (= (:checkbox/title old-checkbox)
                                                                                                          checkbox-title)
                                                                                                       (:checkbox/checked old-checkbox))))
                                                                                            false
                                                                                            old-checkboxes)))))))
                                       card-checkboxes)
        checkbox-order (atom 0)
        upsertion-data (into [] (concat [{:db/id document-id
                                          :card/id-symbol card-id-symbol
                                          :card/title card-title
                                          :card/tenant tenant
                                          :card/deleted (or card-deleted false)}]
                                        (for [{checkbox-id-str :checkbox-id
                                               checkbox-title :checkbox-title
                                               checkbox-disabled :checkbox-disabled
                                               checkbox-checked :checkbox-checked
                                               checkbox-deleted :checkbox-deleted} card-checkboxes-to-upsert
                                              :when (not (.contains (map :checkbox/title checkboxes-to-delete)
                                                                    checkbox-title))]
                                          {:checkbox/id [card-id-symbol checkbox-id-str tenant]
                                           :checkbox/card-id document-id
                                           :checkbox/id-str checkbox-id-str
                                           :checkbox/tenant tenant
                                           :checkbox/deleted (or checkbox-deleted false)
                                           :checkbox/order (swap! checkbox-order inc)
                                           :checkbox/title checkbox-title
                                           :checkbox/disabled (or checkbox-disabled false)
                                           :checkbox/checked (or checkbox-checked false)})
                                        checkboxes-to-delete))]
    (datascript/transact! checklist-conn
                          upsertion-data)))


(defn delete-card! [tenant card]
  (let [document-id (or (get-card-document-id tenant card) -1)]
    (upsert-card! tenant (assoc card :card-deleted true))
    (datascript/transact! checklist-conn
                          (map #(assoc % :checkbox/deleted true)
                               (get-old-checkboxes tenant document-id)))))


(defn get-cards [tenant]
  (let [card-arguments 3
        cards (datascript/q '[:find ?card-id-symbol ?card-title ?card-tenant ?checkbox-order ?checkbox-id-str ?checkbox-title ?checkbox-disabled ?checkbox-checked
                              :in $ ?card-tenant %
                              :where
                              [?card :card/id-symbol ?card-id-symbol]
                              [?card :card/title ?card-title]
                              [?card :card/tenant ?card-tenant]
                              [?card :card/deleted false]
                              [?checkbox :checkbox/card-id ?card]
                              [?checkbox :checkbox/order ?checkbox-order]
                              [?checkbox :checkbox/id-str ?checkbox-id-str]
                              [?checkbox :checkbox/deleted false]
                              [?checkbox :checkbox/title ?checkbox-title]
                              [?checkbox :checkbox/disabled ?checkbox-disabled]
                              [?checkbox :checkbox/checked ?checkbox-checked]]
                            @checklist-conn
                            tenant)]
    (for [card (group-by (partial take card-arguments) cards)]
      (let [[[card-id-symbol card-title card-tenant] checkboxes] card]
        {:card-id card-id-symbol
         :card-title card-title
         :card-tenant card-tenant
         :card-checkboxes (for [checkbox (sort-by #(nth % card-arguments) checkboxes)]
                            (let [[checbox-order checkbox-id-str checkbox-title checkbox-disabled checkbox-checked] (drop card-arguments checkbox)]
                              {:checkbox-id checkbox-id-str
                               :checkbox-title checkbox-title
                               :checkbox-disabled checkbox-disabled
                               :checkbox-checked checkbox-checked}))}))))


(defn- get-cards-string-document-id [tenant]
  (ffirst (datascript/q '[:find ?card
                          :in $ ?card-tenant
                          :where
                          [?card :cards-string/tenant ?card-tenant]]
                        @checklist-conn
                        tenant)))


(defn upsert-cards-string! [tenant body]
  (let [document-id (or (get-cards-string-document-id tenant) -1)
        formatted-string (cljfmt.core/reformat-string body)
        old-cards (get-cards tenant)
        evaluated-cards-map (into {} (map #(vector (:card-id %) %)
                                          (cards-spec/evaluate-expr formatted-string)))
        upsertion-data [{:db/id document-id
                         :cards-string/body formatted-string
                         :cards-string/tenant tenant}]]
    (datascript/transact! checklist-conn
                          upsertion-data)
    (loop [[old-card & rest-old-cards] old-cards]
      (when old-card
        (when-not (.contains (keys evaluated-cards-map)
                             (:card-id old-card))
          (delete-card! tenant old-card)))
      (when rest-old-cards
        (recur rest-old-cards)))
    (loop [[new-card & rest-new-cards] (vals evaluated-cards-map)]
      (when new-card
        (binding [*drop-old-checkbox-state* false]
          (upsert-card! tenant new-card)))
      (when rest-new-cards
        (recur rest-new-cards)))
    formatted-string))


(defn get-cards-string [tenant]
  (let [body (ffirst (datascript/q '[:find ?card-body
                                     :in $ ?card-tenant
                                     :where
                                     [?card :cards-string/body ?card-body]
                                     [?card :cards-string/tenant ?card-tenant]]
                                   @checklist-conn
                                   tenant))]
    (or body
        (str "(defcard sample-card \"Sample Card\"" \newline
             "  (check \"Hello, world\")" \newline
             "  (auto \"This is a test!\" true))" \newline))))


(defn get-context-value [tenant context-id-str]
  (let [value (ffirst (datascript/q '[:find ?context-value
                                      :in $ [?context-tenant ?context-id-str]
                                      :where
                                      [?context :context/value ?context-value]
                                      [?context :context/id-str ?context-id-str]
                                      [?context :context/tenant ?context-tenant]]
                                    @checklist-conn
                                    [tenant context-id-str]))]
    (or value
        false)))


(defn- get-context-document-id [tenant context-id-str]
  (ffirst (datascript/q '[:find ?context
                          :in $ [?context-tenant ?context-id-str]
                          :where
                          [?context :context/id-str ?context-id-str]
                          [?context :context/tenant ?context-tenant]]
                        @checklist-conn
                        [tenant context-id-str])))


(defn upsert-context! [tenant context-id-str value]
  (let [document-id (or (get-context-document-id tenant context-id-str) -1)
        upsertion-data [{:db/id document-id
                         :context/id-str context-id-str
                         :context/value value
                         :context/tenant tenant}]]
    (datascript/transact! checklist-conn
                          upsertion-data)))


(defn- get-schedule-string-document-id [tenant]
  (ffirst (datascript/q '[:find ?schedule
                          :in $ ?schedule-tenant
                          :where
                          [?schedule :schedule-string/tenant ?schedule-tenant]]
                        @checklist-conn
                        tenant)))


(defn get-schedules [tenant]
  (let [schedules (datascript/q '[:find ?schedule-order ?schedule-id-str ?schedule-type ?schedule-card ?schedule-context ?schedule-schedule
                                  :in $ ?schedule-tenant %
                                  :where
                                  [?schedule :schedule/id-str ?schedule-id-str]
                                  [?schedule :schedule/schedule-type ?schedule-type]
                                  [?schedule :schedule/schedule-card ?schedule-card]
                                  [?schedule :schedule/schedule-context ?schedule-context]
                                  [?schedule :schedule/schedule-schedule ?schedule-schedule]
                                  [?schedule :schedule/deleted false]
                                  [?schedule :schedule/order ?schedule-order]
                                  [?schedule :schedule/tenant ?schedule-tenant]]
                                @checklist-conn
                                tenant)]
    (for [[schedule-order schedule-id-str schedule-type schedule-card schedule-context schedule-schedule] schedules]
      {:schedule-id schedule-id-str
       :schedule-type schedule-type
       :schedule-card schedule-card
       :schedule-context schedule-context
       :schedule schedule-schedule})))


(defn upsert-schedule-string! [tenant body]
  (let [document-id (or (get-schedule-string-document-id tenant) -1)
        formatted-string (cljfmt.core/reformat-string body)
        old-schedules (get-schedules tenant)
        evaluated-schedules (schedule-spec/evaluate-expr formatted-string)
        schedules-to-delete (map #(assoc % :schedule/deleted true)
                                 (filter #(not (.contains (map :schedule-id evaluated-schedules)
                                                          (:schedule/id-str %)))
                                         old-schedules))
        schedule-order (atom 0)
        upsertion-data (into [] (concat [{:db/id document-id
                                          :schedule-string/body formatted-string
                                          :schedule-string/tenant tenant}]
                                        (for [{schedule-id-str :schedule-id
                                               schedule-type :schedule-type
                                               schedule-card :schedule-card
                                               schedule-context :schedule-context
                                               schedule :schedule} evaluated-schedules
                                              :when (not (.contains (map :schedule/id-str schedules-to-delete)
                                                                    schedule-id-str))]
                                          {:schedule/id-str schedule-id-str
                                           :schedule/schedule-type schedule-type
                                           :schedule/schedule-card (or schedule-card false)
                                           :schedule/schedule-context (or schedule-context false)
                                           :schedule/schedule-schedule schedule
                                           :schedule/deleted false
                                           :schedule/tenant tenant
                                           :schedule/string-id document-id
                                           :schedule/order (swap! schedule-order inc)})
                                        schedules-to-delete))]
    (datascript/transact! checklist-conn
                          upsertion-data)
    formatted-string))


(defn get-schedule-string [tenant]
  (let [body (ffirst (datascript/q '[:find ?schedule-body
                                     :in $ ?schedule-tenant
                                     :where
                                     [?schedule :schedule-string/body ?schedule-body]
                                     [?schedule :schedule-string/tenant ?schedule-tenant]]
                                   @checklist-conn
                                   tenant))]
    (or body
        (str "(reset sample-card (each-minute))\n"))))


(defn init-tenant! [tenant]
  (when-not (ffirst (datascript/q '[:find ?tenant
                                    :in $ ?tenant
                                    :where
                                    [?c :card/tenant ?tenant]
                                    [?cs :cards-string/tenant ?tenant]
                                    [?ss :schedule-string/tenant ?tenant]]
                                  @checklist-conn
                                  tenant))
    (upsert-cards-string! tenant
                          (get-cards-string tenant))
    (upsert-schedule-string! tenant
                             (get-schedule-string tenant))))
