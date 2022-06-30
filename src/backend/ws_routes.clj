(ns backend.ws-routes
  (:require [schema.core :as s]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]

            [honey.sql :as hsql])
  (:import [java.sql Timestamp]
           [java.time Instant]))


(def ws-request-routes
  {:patients/data   
    (fn [ctx where limit offset]
     (let [{db-spec :db-spec} ctx
           query {:select [:uuid :resource]
                   :from [:patients]
                   :where [:= nil]}]
          (jdbc/query db-spec (hsql/format query) {:keywordize? false})))

   :patients/create
     (fn [ctx resource]
       (let [{db-spec :db-spec} ctx
             uuid (java.util.UUID/randomUUID)]
         (jdbc/insert! db-spec :patients {:uuid uuid
                                        :deleted nil        
                                        :resource resource}) uuid))
   
   :patients/update
     (fn [{db-spec :db-spec}
          {uuid :uuid resource :resource}]
         (jdbc/update! db-spec :patients
                     {:resource resource} ["uuid = ?::uuid" uuid]))
   
   :patients/delete
     (fn [{db-spec :db-spec} uuid]
         (jdbc/update! db-spec :patients
            {:deleted (Timestamp/from (Instant/now))} ["uuid = ?::uuid" uuid]))})
