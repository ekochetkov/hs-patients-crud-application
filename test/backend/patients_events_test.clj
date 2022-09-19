(ns backend.patients-events-test
  (:require
   [backend.context :refer [ctx]]
   [backend.ws :as ws :refer [ws-process-event]]
   [clojure.set :refer [intersection]]
;;   [backend.ws-events :refer [process-ws-event]]   
   [backend.patients-events :as pe]
   [generators.patient :refer [gen-fake-patient
                               gen-unique-fake-patients]]      
   [backend.db]
   [schema.core :as schema]
   [clojure.java.jdbc :as jdbc]
   [clojure.test :refer [deftest is use-fixtures]]))

;; Check result schemas

(def create-event-result-schema
  java.util.UUID)

(def update-event-result-schema
  java.util.UUID)  

(def delete-event-result-schema
  :success-deleted)

(def read-event-result-schema
  {(schema/required-key :total)       schema/Int
   (schema/required-key :page-number) schema/Int
   (schema/required-key :page-size)   schema/Int
   (schema/required-key :rows)        (schema/maybe [{schema/Keyword schema/Any}])})

(deftest create-event-result-schema-test
  (let [patient      (:db (gen-fake-patient))
        event        [:patients/create patient]
        event-result (ws/ws-process-event ctx event)]    
    (is (schema/validate create-event-result-schema
                         event-result))))

(deftest update-event-result-schema-test
  (let [patient (:db (gen-fake-patient))        
        uuid    (ws/ws-process-event ctx
                                     [:patients/create patient])
        event-result (ws/ws-process-event ctx
                                          [:patients/update uuid {}])]
    (is (schema/validate update-event-result-schema
                         event-result))))

(deftest delete-event-result-schema-test
  (let [patient      (:db (gen-fake-patient))        
        uuid         (ws/ws-process-event ctx
                                     [:patients/create patient])
        event-result (ws/ws-process-event ctx
                                          [:patients/delete uuid])]
    (is (= delete-event-result-schema
           event-result))))

(deftest read-event-empty-result-schema-test
  (let [event        [:patients/read]
        event-result (ws/ws-process-event ctx event)]
    (is (schema/validate read-event-result-schema
                         event-result))))

(deftest read-event-not-empty-result-schema-test
  (ws/ws-process-event ctx [:patients/create (:db (gen-fake-patient))])
  (let [event        [:patients/read]
        event-result (ws/ws-process-event ctx event)]
    (is (schema/validate read-event-result-schema
                         event-result))))

(when (nil? (:db-spec ctx))
  (throw (Exception. "Need env var 'DATABASE_URL'")))

(defn- count-table-patients [sys]
  (:count (first (jdbc/query (:db-spec sys)"select count(*) from patients"))))

(defn- table-patients-empty? [sys]
  (= (:count (first (jdbc/query (:db-spec sys)"select count(*) from patients")))
     0))

(defn- get-by-id [{db-spec :db-spec} uuid]
  (first (jdbc/query db-spec ["select * from patients where uuid = ?" uuid])))

(defn fx-clear-db [f]    
  (jdbc/execute! (:db-spec ctx) "delete from patients")
  (f))

(use-fixtures :each fx-clear-db)

;; Create

(deftest positive-create-patient
  (let [patient (:db (gen-fake-patient))
        event [:patients/create patient]
        inserted-uuid (ws/ws-process-event ctx event)
        inserted-row (get-by-id ctx inserted-uuid)]
    (is (= patient
           (:resource inserted-row)))))

(deftest negative-create-patient
  (let [event [:patients/create {}]]
    (is (thrown? Exception (ws/ws-process-event ctx event)))
    (is table-patients-empty?)))

;; TODO: negative double

;; Update

(deftest positive-update-patient
  (let [resource (:db (gen-fake-patient))
        modified-fields {"patient_name" "Some new name"
                         "address" "Some new address"}
        uuid (ws-process-event ctx [:patients/create resource])
        exist-row (get-by-id ctx uuid)
        uuid (ws-process-event ctx [:patients/update uuid modified-fields])
        updated-row (get-by-id ctx uuid)]

    (is (not= exist-row updated-row))
    (is (= (merge resource modified-fields)
           (:resource updated-row)))))

(deftest negative-update-not-exists-patient
  (let [modified-fields {}
        rnd-uuid (java.util.UUID/randomUUID)
        update-event [:patients/update rnd-uuid modified-fields]]
    (is (thrown? Exception
                 (ws-process-event ctx update-event)))))

(deftest negative-update-incorrect-data
  (let [resource (:db (gen-fake-patient))
        modified-fields {"address" nil}
        uuid (ws-process-event ctx [:patients/create resource])
        update-event [:patients/update uuid modified-fields]]
    (is (thrown? Exception
                 (ws-process-event ctx update-event)))))

;; Delete

(deftest positive-delete-patient
  (let [uuid (ws-process-event ctx [:patients/create (:db (gen-fake-patient))])
        created-row (get-by-id ctx uuid)                                    
        deleted-result (ws/ws-process-event ctx [:patients/delete uuid])
        deleted-row (get-by-id ctx uuid)]

    (is created-row)
    (is (= deleted-result :success-deleted))
    (is (inst? (get deleted-row :deleted)))
    (is (= (count-table-patients ctx) 1))))

(deftest negative-delete-not-exists-patient
  (let [rnd-uuid (java.util.UUID/randomUUID)
        delete-event [:patients/delete rnd-uuid]]
    (is (thrown? Exception
                 (ws-process-event ctx delete-event)))))

(deftest negative-delete-already-deleted-patient
  (let [uuid (ws-process-event ctx [:patients/create (:db (gen-fake-patient))])
        delete-event [:patients/delete uuid]]
    (ws/ws-process-event ctx delete-event)    
    (is (thrown? Exception
                 (ws-process-event ctx delete-event)))))

;; Read. where

(deftest build-where-conditions-default
  (is (= [:and [:= :deleted nil]]
         (pe/build-where {}))))

(deftest build-where-some-conditions
  (let [conds [ [:=  "patient_name" "A"]
                [:<= "birth_date" 10] ]]
    (is (=
         [:and [:= :deleted nil]
               [:= [:raw "(resource->>'patient_name')::text"] "A"]
               [:<= [:raw "(resource->>'birth_date')::bigint"] 10]          ]
         (pe/build-where conds)))))

(deftest build-data-query-default
  (is (= (pe/build-data-query {} "where conds here")
         {:select [:uuid :resource]
          :from [:patients]
          :where "where conds here"
          :limit 30
          :offset 0})))

(deftest build-data-query-set-page-and-size
  (is (= (pe/build-data-query {:page-size 45 :page-number 5} ["where conds here"])
         {:select [:uuid :resource]
          :from [:patients]
          :where ["where conds here"]
          :limit 45
          :offset 180})))

(deftest build-data-query-page-size-over-global-limit
  (is (= (pe/build-data-query {:page-size 450} ["where conds here"])
         {:select [:uuid :resource]
          :from [:patients]
          :where ["where conds here"]
          :limit pe/read-global-limit
          :offset 0})))

(deftest build-count-query
  (is (= (pe/build-count-query ["where conds here"])
         {:select [[:%count.*]]
          :from [:patients]
          :where ["where conds here"]})))

;; Read

(deftest positive-simple-read
  (let [total (inc (rand-int 10))
        patients (map :db (gen-unique-fake-patients total))]
        (doall (->> patients
               (map #(ws-process-event ctx [:patients/create %]))))
     (is (= total (count-table-patients ctx)))
     (let [read-event [:patients/read {:page-size total}]
           result (ws-process-event ctx read-event)
           all-resources (->> result
                              :rows
                              (map :resource))]
       (is (= ((juxt :total
                     :page-size
                     :page-number) result)
              [total total 1]))
       (is (= (set patients)
              (set all-resources))))))

(deftest positive-read-only-not-deleted-patients
  (let [total (inc (rand-int 10))
        patients (gen-unique-fake-patients total)
        inserted-uuids (doall (->> patients
                                   (map #(ws-process-event ctx [:patients/create (:db %)]))))
        delete (inc (rand-int total))
        uuids-for-delete (take delete inserted-uuids)]

    (is (= (count-table-patients ctx) total))  
    
    (doall (->> uuids-for-delete
                (map #(ws-process-event ctx [:patients/delete %]))))

    (let [read-event [:patients/read {:page-size total}]
          result (ws-process-event ctx read-event)
          viewed-uuids (->> result
                            :rows
                            (map :uuid))]    

      (is (= (+ (count uuids-for-delete)
                (count viewed-uuids))
             total))

      (is (empty? (intersection (set uuids-for-delete)
                                (set viewed-uuids)))))))


(deftest positive-read-pagitator-answer-simple-test
  (let [total    20
        patients (gen-unique-fake-patients total)]
     
    (doall (->> patients
                (map #(ws-process-event ctx [:patients/create (:db %)]))))
    
    (is (= (count-table-patients ctx) total))
    
    (let [r-1    (ws-process-event ctx [:patients/read {:page-number  1 :page-size  5}])
          r-2    (ws-process-event ctx [:patients/read {:page-number  2 :page-size 10}])
          r-over (ws-process-event ctx [:patients/read {:page-number 10 :page-size 40}])]

      (is (= {:total total :page-number 1 :page-size 5}
             (dissoc r-1 :rows)))

      (is (= {:total total :page-number 2 :page-size 10}
             (dissoc r-2 :rows)))

      (is (= {:total total :page-number 1 :page-size 40}
             (dissoc r-over :rows))))))

