(ns acceptance.patients.filter-test
  (:require [clojure.test :refer [deftest use-fixtures join-fixtures]]
            [acceptance.patients.core :as patients-core]
            [acceptance.core :as core]
            [acceptance.context :refer [fx-default]]
            [generators.patient :as patients-gen]))

;;(def driver (e/firefox))
;;(e/go driver "http://localhost:8080/shadow")

(defn prepare-steps [f]
  (patients-core/fn-wait-datagrid-data-changed)
  (f))

(use-fixtures :each (join-fixtures [fx-default prepare-steps]))

(def default-ilike-test-opts
  {:input-field-name     "filter_box"
   :fn-gen-fake-model-** patients-gen/gen-unique-fake-patients
   :fn-insert-row        patients-core/fn-insert-patient   
   :fn-reload-datagrid   patients-core/fn-reload-datagrid
   :fn-get-datagrid-data patients-core/fn-wait-datagrid-data-changed
   :fn-apply-filters     patients-core/fn-apply-filter})

(deftest patient-name-filter-test
  (-> default-ilike-test-opts
      (assoc :model-field-name "patient_name")
      core/ilike-test))

(deftest patient-birth-date-filter-test
  (-> default-ilike-test-opts
      (assoc :model-field-name "birth_date")
      core/ilike-test))

(deftest patient-gender-filter-test
  (-> default-ilike-test-opts
      (assoc :model-field-name "gender")
      core/ilike-test))

(deftest patient-policy-number-filter-test
  (-> default-ilike-test-opts
      (assoc :model-field-name "policy_number")
      core/ilike-test))

(deftest patient-address-filter-test
  (-> default-ilike-test-opts
      (assoc :model-field-name "address")
      core/ilike-test))
