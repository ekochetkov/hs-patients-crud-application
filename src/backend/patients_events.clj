(ns backend.patients-events
  (:require [schema.core :as s]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]
            [backend.ws :refer [process-ws-event]]
            [honey.sql :as hsql])
  (:import [java.sql Timestamp]
           [java.time Instant]))

(defmethod process-ws-event :patients/create
  [ctx _ resource]
    (let [{db-spec :db-spec} ctx
          uuid (java.util.UUID/randomUUID)]
      (jdbc/insert! db-spec :patients
         {:uuid uuid :deleted nil :resource resource}) uuid))
