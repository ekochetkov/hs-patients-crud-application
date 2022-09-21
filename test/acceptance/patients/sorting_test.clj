(ns acceptance.patients.sorting-test
  (:require [clojure.test :refer [deftest use-fixtures join-fixtures]]
            [etaoin.api :as e]
            [backend.ws-events]
            [acceptance.core :as core]            
            [generators.patient :as patients-gen]
            [acceptance.patients.core :as patients-core]            
            [acceptance.context :refer [fx-default *driver*]]))

(defn prepare-steps [f]
  ;; Wait grid load data
  (e/wait-invisible *driver* ".//div[@class='datagrid-mask']")
  (f))

(use-fixtures :each (join-fixtures [fx-default prepare-steps]))

(def default-sorting-test-opts
  {:fn-gen-fake-model-*  patients-gen/gen-fake-patient
   :fn-insert-row        patients-core/fn-insert-patient   
   :fn-reload-datagrid   patients-core/fn-reload-datagrid
   :fn-get-datagrid-data patients-core/fn-wait-datagrid-data-changed
   :fn-apply-sorting     patients-core/fn-apply-sorting})

(deftest patients-patient_name-sorting-test
  (-> default-sorting-test-opts
      (assoc :field "patient_name")
      core/common-sorting-test))

(deftest patients-gender-sorting-test
  (-> default-sorting-test-opts
      (assoc :field "gender")
      core/common-sorting-test))

(deftest patients-birth_date-sorting-test
  (-> default-sorting-test-opts
      (assoc :field "birth_date")
      core/common-sorting-test))

(deftest patients-policy_number-sorting-test
  (-> default-sorting-test-opts
      (assoc :field "policy_number")
      core/common-sorting-test))

(deftest patients-address-sorting-test
  (-> default-sorting-test-opts
      (assoc :field "address")
      core/common-sorting-test))

