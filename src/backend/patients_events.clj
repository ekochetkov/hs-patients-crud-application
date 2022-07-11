(ns backend.patients-events
  (:require [schema.core :as s]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]
            [backend.ws-events :refer [process-ws-event]]
            [common.patients]
            [backend.db :as db]
            [honey.sql :as hsql])
  (:import [java.sql Timestamp]
           [java.time Instant]))

(def db-resource-schema
  {(s/required-key "patient_name") (s/conditional
          (get-in common.patients/validation-rules ["patient_name" "rule" "validator"]) s/Str)
   (s/required-key "policy_number") (s/conditional
          (get-in common.patients/validation-rules ["policy_number" "rule" "validator"]) s/Str)
   (s/required-key "birth_date") s/Int
   (s/required-key "gender") (s/enum "male" "female")
   (s/required-key "address") s/Str})

(def db-row-schema
  {(s/required-key :uuid) java.util.UUID
   (s/required-key :deleted) (s/maybe s/Int)
   (s/required-key :resource) db-resource-schema})

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
  [ctx _ [where page-number page-size]]
  (let [{db-spec :db-spec} ctx
        base-where [[:= :deleted nil]]
        fields-type-cast {"birth_date" "bigint"}
        resource-where (map (fn [[op field & args]]
                         (into [op (db/pg->> "resource" field (get fields-type-cast field "text"))] args)) where)
        query-data {:select [:uuid :resource]
                    :from [:patients]
                    :where (concat [:and] base-where resource-where)
                    :limit page-size
                    :offset (* page-size (dec page-number))}
        query-count {:select [[:%count.*]]
                     :from [:patients]
                     :where (concat [:and] base-where resource-where)}]
    [(->
        (jdbc/query db-spec (hsql/format query-count) {:keywordize? false})
        first
        :count)
      page-number
      page-size
      (jdbc/query db-spec (hsql/format query-data) {:keywordize? false})]))
