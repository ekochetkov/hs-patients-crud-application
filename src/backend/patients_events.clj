(ns backend.patients-events
  (:require [schema.core :as s]
            [clojure.java.jdbc :as jdbc]
            [backend.ws :refer [ws-process-event]]
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

;(def sys {:db-spec (System/getenv "DATABASE_URL")})

;;(jdbc/query db-spec
;;          ["select * from patients"])

(defn patient-find-by-uuid [sys uuid]
  (first (jdbc/query (:db-spec sys)
            ["select * from patients where uuid = ? limit 1" uuid])))

(defn patient-not-exists? [sys uuid]
  (nil? (patient-find-by-uuid sys uuid)))

(def patient-exists?
  (complement patient-not-exists?))

(defn unique? [sys row]
  (let [uuid          (:uuid row)
        policy_number (get-in row [:resource "policy_number"])
        rows          (jdbc/query (:db-spec sys)
                         ["select * from patients where (resource->>'policy_number')::text = ?" policy_number])]
    (or (empty? rows)
        (and (= 1 (count rows))
             (= uuid
                (-> rows first :uuid))))))

(defmethod ws-process-event :patients/create
  [sys [_ resource]]
  (let [uuid (java.util.UUID/randomUUID)]
    (if (patient-exists? sys uuid)
      (recur sys [:patients/create resource])
      (let [row-for-insert {:uuid     uuid
                            :deleted  nil
                            :resource resource}]
        (s/validate db-row-schema row-for-insert)
        (if (unique? sys row-for-insert)
          (do
            (jdbc/insert! (:db-spec sys) :patients row-for-insert)
            {:success true
             :uuid    uuid})
          {:success false
           :rules {"policy_number" :double-policy_number}})))))
    
(defmethod ws-process-event :patients/update
  [sys [_ uuid modified-fields]]
  (let [{db-spec :db-spec} sys
        source-row (first (jdbc/query db-spec
          ["select * from patients where uuid = ?" uuid]))        
        target-row (assoc-in source-row [:resource]
          (merge (:resource source-row) modified-fields))]

    (when (nil? source-row)
      (throw (Exception. (str "Record for " uuid " not found"))))
    
    (s/validate db-row-schema target-row)
        (if (unique? sys target-row)
          (do
            (jdbc/update! db-spec :patients {:resource (:resource target-row)} ["uuid = ?" uuid])
            {:success true
             :uuid    uuid})
          {:success false
           :rules {"policy_number" :double-policy_number}})))

(defmethod ws-process-event :patients/delete
  [ctx [_ uuid]]
  (let [{db-spec :db-spec} ctx
        select-query ["select * from patients where uuid = ?" uuid]
        deleted-row (first (jdbc/query db-spec select-query))]
    
    (when (nil? deleted-row)
      (throw (Exception. (str "Record for " uuid " not found"))))

    (when (inst? (:deleted deleted-row))
      (throw (Exception. (format "Record %s already deleted at %s"
                                 uuid
                                 (:deleted deleted-row)))))    
    
     (jdbc/update! db-spec :patients
                   {:deleted (Timestamp/from (Instant/now))} ["uuid = ?::uuid" uuid]))
  :success-deleted)

(def fields-type-cast {"birth_date" "bigint"})

(defn build-where [front-where]
    (->> front-where
         (map (fn [[op field & args]]
                (let [field-pg-type (get fields-type-cast field "text")]
                  (into [op (db/pg->> "resource" field field-pg-type)] args))))
         (concat [:and [:= :deleted nil]])
         vec))

(def read-global-limit 100)

(defn build-data-query [opts where]
  (let [page-number    (or (:page-number opts) 1)
        page-size-need (or (:page-size opts) 30)
        page-size      (if (< page-size-need read-global-limit)
                         page-size-need
                         read-global-limit)
        order-by       (or (:order-by opts) [["patient_name" :asc]])]
    {:select [:uuid :resource]
     :from   [:patients]
     :where  where
     :order-by (->> order-by
                    (mapv (fn [[field direction]]
                            (let [field-pg-type (get fields-type-cast field "text")]
                               [(db/pg->> "resource" field field-pg-type) direction]))))
     :limit  page-size
     :offset (* page-size (dec page-number))}))

(defn build-count-query [where]
  {:select [[:%count.*]]
   :from [:patients]
   :where where})

(defmethod ws-process-event :patients/read
  [ctx [_ opts]]
  (let [where       (build-where (or (:where opts) {}))
        data-query  (build-data-query opts where)
        count-query (build-count-query where)
        rows        (jdbc/query (:db-spec ctx)
                         (hsql/format data-query)
                         {:keywordize? false})
        total       (-> (jdbc/query (:db-spec ctx)
                                    (hsql/format count-query))
                        first
                        :count)
        page-size   (:limit data-query)
        page-number (let [req-page-number (inc (/ (:offset data-query)
                                                  (:limit  data-query)))
                          max-page-number (inc (int (/ total
                                                       (:limit data-query))))]
                      (if (<= req-page-number
                              max-page-number)
                        req-page-number
                        max-page-number))]
    {:total       total
     :page-number page-number     
     :page-size   page-size
     :rows        rows}))
