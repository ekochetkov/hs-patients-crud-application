(ns backend.patients-events-test
  (:require
   [backend.ws :as ws]
   [backend.db]
   [clojure.java.jdbc :as jdbc]
   [clojure.test :refer [deftest is]])))

(def ctx {:db-spec (System/getenv "TEST_DATABASE_URL")})

(defn- clear-table-patients [{db-spec :db-spec}]
  (jdbc/execute! db-spec "delete from patients"))

(deftest positive-create-patient
  (clear-table-patients ctx)
(let [request {:data [:patients/create {"gender" "male"
                                        "address" "New Zealand, Taranaki, Taupo, Bucs Road st. 2296" 
                                        "birth_date" 702604800
                                        "patient_name" "Brad Morris"
                                        "policy_number" 5492505115922541}]}
        result (ws/ws-process-request ctx (str request))
        count (first (jdbc/query (:db-spec ctx) "select count(*) from patients"))]
    (is (= (type (-> result :data second))
           java.util.UUID))
    (is (= (:count count) 1))))

