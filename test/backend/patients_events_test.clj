(ns backend.patients-events-test
  (:require
   [backend.ws :as ws]
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
        result (ws/ws-process-request ctx (str request))
        inserted-uuid (-> result :data second)
        inserted-row (get-by-id ctx inserted-uuid)]
    
    (when (= (-> result :data first) :comm/error)
      (throw (Exception. (str result))))

    (is (= resource (:resource inserted-row)))))

(deftest negative-create-patient
  (clear-table-patients ctx)
  (let [resource {}
        request {:data [:patients/create resource]}
        result (ws/ws-process-request ctx (str request))]
    (is (= (-> result :data first) :comm/error))
    (is (= (count-table-patients ctx) 0))))
