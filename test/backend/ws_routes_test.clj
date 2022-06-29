(ns backend.ws-routes-test
  (:require
   [clojure.java.jdbc :as jdbc]
   [backend.ws :refer [prcess-]]
   [backend.ws-routes :refer [ws-request-routes]]   
   [clojure.test :refer [deftest is]]))

(def ctx {:db-spec (System/getenv "TEST_DATABASE_URL")})

(defn clear-table-patients [{db-spec :db-spec}]
  (jdbc/execute! db-spec "delete from patients"))

(deftest create-patients-with-correct-data
  (let [request-uuid (java.util.UUID/randomUUID)
        request {:proto :request
                 :id request-uuid
                 :data [:patients/create {"patient_name" ""
                                          "gender" "gender_data"
                                          "address" "addres_data"
                                          "policy_number" "policy_number_data"

        (clear-table-patients ctx)

        
        
  (is (= 1 2)))
