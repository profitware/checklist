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
                       :checkbox/title {:db/cardinality :db.cardinality/one}
                       :checkbox/disabled {:db/cardinality :db.cardinality/one}
                       :checkbox/checked {:db/cardinality :db.cardinality/one}
                       :checkbox/tenant {:db/cardinality :db.cardinality/one}})


(def checklist-conn (datascript/create-conn checklist-schema))


(defn- get-card-document-id [card]
  (let [{card-id-symbol :card-id} card
        tenant "default"]
    (ffirst (datascript/q '[:find ?card
                            :in $ [?card-id-symbol ?card-tenant]
                            :where
                            [?card :card/id-symbol ?card-id-symbol]
                            [?card :card/tenant ?card-tenant]]
                          @checklist-conn
                          [card-id-symbol tenant]))))


(defn upsert-card! [card]
  (let [{card-id-symbol :card-id
         card-title :card-title
         card-checkboxes :card-checkboxes} card
        tenant "default"
        document-id (or (get-card-document-id card) -1)
        upsertion-data (into [] (concat [{:db/id document-id
                                          :card/id-symbol card-id-symbol
                                          :card/title card-title
                                          :card/tenant tenant}]
                                        (for [{checkbox-id-str :checkbox-id
                                               checkbox-title :checkbox-title
                                               checkbox-disabled :checkbox-disabled
                                               checkbox-checked :checkbox-checked} card-checkboxes]
                                          {:checkbox/card-id document-id
                                           :checkbox/id-str checkbox-id-str
                                           :checkbox/title checkbox-title
                                           :checkbox/disabled (or checkbox-disabled false)
                                           :checkbox/checked (or checkbox-checked false)})))]
    (datascript/transact! checklist-conn
                          upsertion-data)))


(defn get-cards []
  (let [card-arguments 3
        tenant "default"
        cards (datascript/q '[:find ?card-id-symbol ?card-title ?card-tenant ?checkbox-id-str ?checkbox-title ?checkbox-disabled ?checkbox-checked
                              :in $ ?card-tenant %
                              :where
                              [?card :card/id-symbol ?card-id-symbol]
                              [?card :card/title ?card-title]
                              [?card :card/tenant ?card-tenant]
                              [?checkbox :checkbox/card-id ?card]
                              [?checkbox :checkbox/id-str ?checkbox-id-str]
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
         :card-checkboxes (for [checkbox checkboxes]
                            (let [[checkbox-id-str checkbox-title checkbox-disabled checkbox-checked] (drop card-arguments checkbox)]
                              {:checkbox-id checkbox-id-str
                               :checkbox-title checkbox-title
                               :checkbox-disabled checkbox-disabled
                               :checkbox-checked checkbox-checked}))}))))


(defn- get-cards-string-document-id []
  (let [tenant "default"]
    (ffirst (datascript/q '[:find ?card
                            :in $ ?card-tenant
                            :where
                            [?card :cards-string/tenant ?card-tenant]]
                          @checklist-conn
                          tenant))))


(defn upsert-cards-string! [body]
  (let [tenant "default"
        document-id (or (get-cards-string-document-id) -1)
        formatted-string (cljfmt.core/reformat-string body)
        evaluated-cards (cards-spec/evaluate-expr formatted-string)
        old-cards (get-cards)
        upsertion-data [{:db/id document-id
                         :cards-string/body formatted-string
                         :cards-string/tenant tenant}]]
    (datascript/transact! checklist-conn
                          upsertion-data)
    (upsert-card! evaluated-card)
    formatted-string))


(defn get-cards-string []
  (let [tenant "default"
        body (ffirst (datascript/q '[:find ?card-body
                                     :in $ ?card-tenant
                                     :where
                                     [?card :cards-string/body ?card-body]
                                     [?card :cards-string/tenant ?card-tenant]]
                                   @checklist-conn
                                   tenant))]
    (or body
        (str "(defcard sample-card \"Sample Card\"" \newline
             "  (checkbox \"Hello, world\")" \newline
             "  (auto \"This is a test!\" true))" \newline))))
