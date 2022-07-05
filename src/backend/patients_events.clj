(ns backend.patients-events
  (:require [schema.core :as s]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]
            [backend.ws :refer [process-ws-event]]
            [common.patients :refer [db-row-schema]]
            [backend.db :as db]
            [honey.sql :as hsql])
  (:import [java.sql Timestamp]
           [java.time Instant]))

(defmethod process-ws-event :patients/create
  [ctx _ [resource]]
    (let [{db-spec :db-spec} ctx
          uuid (java.util.UUID/randomUUID)
          row-for-insert{ :uuid uuid :deleted nil :resource resource}]
      (s/validate db-row-schema row-for-insert)      
      (jdbc/insert! db-spec :patients row-for-insert) uuid))

(defmethod process-ws-event :patients/update
  [ctx _ [uuid modified-fields]]
  (let [{db-spec :db-spec} ctx
        source-row (first (jdbc/query db-spec
          ["select * from patients where uuid = ?" uuid]))        
        target-row (assoc-in source-row [:resource]
          (merge (:resource source-row) modified-fields))]
      (s/validate db-row-schema target-row)      
      (jdbc/update! db-spec :patients {:resource (:resource target-row)} ["uuid = ?" uuid]) uuid))

(defmethod process-ws-event :patients/delete
  [ctx _ [uuid]]
  (let [{db-spec :db-spec} ctx]
     (jdbc/update! db-spec :patients
        {:deleted (Timestamp/from (Instant/now))} ["uuid = ?::uuid" uuid])))

(defmethod process-ws-event :patients/read
  [ctx _ [where limit offset]]
  (let [{db-spec :db-spec} ctx
        base-where [[:= :deleted nil]]
        fields-type-cast {"birth_date" "bigint"}
        resource-where (map (fn [[op field & args]]
                         (into [op (db/pg->> "resource" field (get fields-type-cast field "text"))] args)) where)
        query {:select [:uuid :resource]
               :from [:patients]
               :where (concat [:and] base-where resource-where)
               :limit 25}]
       (jdbc/query db-spec (hsql/format query) {:keywordize? false})))
