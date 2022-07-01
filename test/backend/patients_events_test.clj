(ns backend.patients-events-test
  (:require
   [backend.ws :as ws :refer [process-ws-event]]
   [backend.db]
   [clojure.java.jdbc :as jdbc]
   [clojure.test :refer [deftest is]]))

(def ctx {:db-spec (System/getenv "TEST_DATABASE_URL")})

(defn- clear-table-patients [{db-spec :db-spec}]
  (jdbc/execute! db-spec "delete from patients"))

(defn- count-table-patients [{db-spec :db-spec}]
  (:count (first (jdbc/query (:db-spec ctx)"select count(*) from patients"))))

(defn- get-by-id [{db-spec :db-spec} uuid]
  (first (jdbc/query db-spec ["select * from patients where uuid = ?" uuid])))

(deftest positive-create-patient
  (clear-table-patients ctx)
  (let [resource {"gender" "male"
                  "address" "New Zealand, Taranaki, Taupo, Bucs Road st. 2296" 
                  "birth_date" 702604800
                  "patient_name" "Brad Morris"
                  "policy_number" "5492505115922541"}
        request {:data [:patients/create resource]}
        response (ws/ws-process-request ctx (str request))
        inserted-uuid (-> response :data second)
        inserted-row (get-by-id ctx inserted-uuid)]
    
    (when (= (-> response :data first) :comm/error)
      (throw (Exception. (str response))))

    (is (= resource (:resource inserted-row)))))

(deftest negative-create-patient
  (clear-table-patients ctx)
  (let [resource {}
        request {:data [:patients/create resource]}
        response (ws/ws-process-request ctx (str request))]
    (is (= (-> response :data first) :comm/error))
    (is (= (count-table-patients ctx) 0))))

(deftest positive-update-patient
  (clear-table-patients ctx)  
  (let [modified-fields {"patient_name" "Some new name"
                         "address" "Some new address"}
        resource {"gender" "male"
                  "address" "New Zealand, Taranaki, Taupo, Bucs Road st. 2296" 
                  "birth_date" 702604800
                  "patient_name" "Brad Morris"
                  "policy_number" "5492505115922541"}
        uuid (process-ws-event ctx :patients/create [resource])
        request {:data [:patients/update uuid modified-fields]}
        response (ws/ws-process-request ctx (str request))
        updated-row (get-by-id ctx uuid)]

    (when (= (-> response :data first) :comm/error)
      (throw (Exception. (str response))))
    
    (is (= (merge resource modified-fields)
           (:resource updated-row)))))

(deftest negative-update-patient
  (clear-table-patients ctx)  
  (let [modified-fields {"address" nil}
        resource {"gender" "male"
                  "address" "New Zealand, Taranaki, Taupo, Bucs Road st. 2296" 
                  "birth_date" 702604800
                  "patient_name" "Brad Morris"
                  "policy_number" "5492505115922541"}
        uuid (process-ws-event ctx :patients/create [resource])
        request {:data [:patients/update uuid modified-fields]}
        response (ws/ws-process-request ctx (str request))
        updated-row (get-by-id ctx uuid)]

    (is (= (-> response :data first) :comm/error))    
    (is (= resource (:resource updated-row)))))


(deftest positive-delete-patient
  (clear-table-patients ctx)
  (let [resource {"gender" "male"
                  "address" "New Zealand, Taranaki, Taupo, Bucs Road st. 2296"
                  "birth_date" 702604800
                  "patient_name" "Brad Morris"
                  "policy_number" "5492505115922541"}
        uuid (process-ws-event ctx :patients/create [resource])
        response (ws/ws-process-request ctx (str {:data [:patients/delete uuid]}))
        added-row (get-by-id ctx uuid)
        deleted-row (get-by-id ctx uuid)]

    (when (= (-> response :data first) :comm/error)
      (throw (Exception. (str response))))

    (is (not (empty? added-row)))
    (is (not (empty (:delted deleted-row))))
    (is (= (count-table-patients ctx) 1))))

(deftest positive-read-only-not-deleted-patients
  (clear-table-patients ctx)
  (let [resource {"gender" "male"
                  "address" "New Zealand, Taranaki, Taupo, Bucs Road st. 2296"
                  "birth_date" 702604800
                  "patient_name" "Brad Morris"
                  "policy_number" "5492505115922541"}
        uuid-1 (process-ws-event ctx :patients/create [resource])
        uuid-2 (process-ws-event ctx :patients/create [resource])
        uuid (process-ws-event ctx :patients/delete [uuid-2])
        response (ws/ws-process-request ctx (str {:data [:patients/read]}))]

    (when (= (-> response :data first) :comm/error)
      (throw (Exception. (str response))))

    (is (= (count-table-patients ctx) 2))
    (is (= (count response) 1))))
