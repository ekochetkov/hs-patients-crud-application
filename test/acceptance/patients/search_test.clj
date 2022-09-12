(ns acceptance.patients.search-test
  (:require [clojure.test :refer [deftest is use-fixtures join-fixtures]]
            [etaoin.api :as e]
            [common.ui-anchors.patients.datagrid :as dg-anchors]
            [common.ui-anchors.patients.core :as c-anchors]
            [common.ui-anchors.patients.search-panel :as sp]            
            [acceptance.patients.core :as patients-core]            
            [acceptance.core :as core]
            [generators.patient :as patients-gen]
            [acceptance.easy-ui.link-button :as link-button]
            [acceptance.easy-ui.layout-panel :as layout-panel]
            [acceptance.easy-ui.form :as form]
            [acceptance.context :refer [fx-default *driver*]]))

;;(def driver (e/firefox))
;;(e/go driver "http://localhost:8080/shadow")

(defn prepare-steps [f]
  ;; Wait grid load data
  (e/wait-invisible *driver* ".//div[@class='datagrid-mask']")
  ;; Open filter panel
  (e/click *driver* {:id dg-anchors/toolbar-search-button})
  ;; Wait filter input ready
  ;; TODO: replace on wait-predicate function
  (e/wait 1)
  (f))

(use-fixtures :each (join-fixtures [fx-default prepare-steps]))

(defn form-reseted? [data]
  (= data
     {"patient_name" ""
      "address" ""
      "policy_number" "____ ____ ____ ____"
      "gender" "any"
      "birth_date_mode" "any"}))

(deftest form-reseted-after-fixtures
  (let [search-form-data (form/get-values sp/form)]
     (is (form-reseted? search-form-data))))

(deftest on-init-after-fixtures
  (is (link-button/selected? dg-anchors/toolbar-search-button))
  (is (layout-panel/opened? c-anchors/search-panel)))

(def default-test-opts
  {:fn-gen-fake-model-** patients-gen/gen-unique-fake-patients
   :fn-insert-row        patients-core/fn-insert-patient   
   :fn-reload-datagrid   patients-core/fn-reload-datagrid
   :fn-get-datagrid-data patients-core/fn-wait-datagrid-data-changed
   :fn-apply-filters     patients-core/fn-apply-search})

(deftest patient-name-search-test
  (-> default-test-opts
      (assoc :model-field-name "patient_name"
             :input-field-name "patient_name")
      core/ilike-test))

(deftest patient-address-search-test
  (-> default-test-opts
      (assoc :model-field-name "address"
             :input-field-name "address")
      core/ilike-test))

(deftest patient-search-gender-male-test
  (-> default-test-opts
      (assoc :fn-target-confine #(= "male" (get-in % [:db "gender"]))
             :search-form {"gender" "male"})      
      core/common-search-test))

(deftest patient-search-gender-female-test
  (-> default-test-opts
      (assoc :fn-target-confine #(= "female" (get-in % [:db "gender"]))
             :search-form {"gender" "female"})      
      core/common-search-test))

(deftest patient-search-policy-number-search
  (let [target-row (patients-gen/gen-fake-patient)
        target-policy-number (get-in target-row [:db "policy_number"])]
    (-> default-test-opts
        (assoc :custom-target-rows (list target-row)
               :fn-target-confine #(= target-policy-number
                                      (get-in % [:db "policy_number"]))
               :search-form {"policy_number" target-policy-number})
        core/common-search-test)))

(deftest patient-birth-date-equal-test
  (let [target-row (patients-gen/gen-fake-patient)
        target-birth-date (get-in target-row [:db "birth_date"])]
    (-> default-test-opts
        (assoc :custom-target-rows (list target-row)
               :fn-target-confine #(= target-birth-date
                                      (get-in % [:db "birth_date"]))
               :search-form {"birth_date_mode" "equal"
                             "birth_date_start" target-birth-date})
        core/common-search-test)))

(deftest patient-birth-date-after-search-test
  (let [some-date (:db (patients-gen/birth_date))]
    (-> default-test-opts
        (assoc :fn-target-confine #(> (get-in % [:db "birth_date"]) some-date)
               :search-form {"birth_date_mode" "after"
                             "birth_date_start" some-date})      
      core/common-search-test)))

(deftest patient-birth-date-before-search-test
  (let [some-date (:db (patients-gen/birth_date))]
    (-> default-test-opts
        (assoc :fn-target-confine #(< (get-in % [:db "birth_date"]) some-date)
               :search-form {"birth_date_mode" "before"
                             "birth_date_start" some-date})      
      core/common-search-test)))

(deftest patient-birth-date-between-search-test
  (let [data-a     (:db (patients-gen/birth_date))
        data-b     (:db (patients-gen/birth_date))
        data_start (min data-a data-b)
        data_end   (max data-a data-b)]
    (-> default-test-opts
        (assoc :fn-target-confine #(and (> (get-in % [:db "birth_date"]) data_start)
                                        (< (get-in % [:db "birth_date"]) data_end))
               :search-form {"birth_date_mode"  "between"
                             "birth_date_start" data_start
                             "birth_date_end"   data_end})
      core/common-search-test)))
