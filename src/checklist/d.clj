(ns checklist.d
  (:require [clojure.tools.logging :as log]
            [environ.core :as environ]
            [datahike.api]
            [datascript.core]))


(def checklist-schema {:card/id {:db/cardinality :db.cardinality/one
                                 :db/index true
                                 :db/unique :db.unique/identity}
                       :card/symbol {:db/cardinality :db.cardinality/one}
                       :card/title {:db/cardinality :db.cardinality/one}
                       :card/tenant {:db/cardinality :db.cardinality/one}
                       :card/hidden {:db/cardinality :db.cardinality/one}
                       :card/highlighted {:db/cardinality :db.cardinality/one}
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
                       :schedule/schedule-type {:db/cardinality :db.cardinality/one}
                       :schedule/schedule-card {:db/cardinality :db.cardinality/one}
                       :schedule/schedule-context {:db/cardinality :db.cardinality/one}
                       :schedule/schedule-schedule {:db/cardinality :db.cardinality/one}
                       :schedule/order {:db/cardinality :db.cardinality/one}
                       :schedule/tenant {:db/cardinality :db.cardinality/one}
                       :schedule/task-id {:db/cardinality :db.cardinality/one}
                       :context/id  {:db/cardinality :db.cardinality/one
                                     :db/index true
                                     :db/unique :db.unique/identity}
                       :context/id-str {:db/cardinality :db.cardinality/one}
                       :context/value {:db/cardinality :db.cardinality/one}
                       :context/tenant {:db/cardinality :db.cardinality/one}})


(def ^:dynamic *db-uri* (let [db-uri (environ/env :checklist-database-uri)]
                          (when-not (empty? db-uri)
                            db-uri)))


(defn get-connection []
  (if *db-uri*
    (do (try (datahike.api/create-database-with-schema *db-uri*
                                                       (assoc checklist-schema
                                                              :schedule/schedule-schedule {:db/cardinality :db.cardinality/many}))
             (catch Exception e
               (when-not (= (:type (ex-data e))
                            :db-already-exists)
                 (throw e))))
        (log/warn (str "DB connection to " *db-uri* " using datahike driver established"))
        (datahike.api/connect *db-uri*))
    (do (log/warn (str "DB connection using datascript driver established"))
        (datascript.core/create-conn checklist-schema))))


(defn q [& args]
  (if *db-uri*
    (apply #'datahike.api/q args)
    (apply #'datascript.core/q args)))


(defn pull-many [& args]
  (if *db-uri*
    (apply #'datahike.api/pull-many args)
    (apply #'datascript.core/pull-many args)))


(defn transact [& args]
  (if *db-uri*
    (apply #'datahike.api/transact args)
    (apply #'datascript.core/transact args)))


(defn release [conn]
  (when *db-uri*
    (datahike.api/release conn)))
