(ns backend.patients-events
  (:require [schema.core :as s]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]
            [backend.ws :refer [process-ws-event]]
            [common.patients :refer [db-row-schema]]
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
