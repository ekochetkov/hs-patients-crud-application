(ns acceptance.patients.create-test
  (:require [clojure.test :refer [deftest is use-fixtures join-fixtures]]
            [etaoin.api :as e]
            [common.ui-anchors.patients.dialog-create :as dc-anchors]
            [acceptance.patients.core :as patients-core]            
            [common.ui-anchors.patients.datagrid :as dg-anchors]
            [acceptance.easy-ui.form :as form]
            [acceptance.easy-ui.link-button :as link-button]
            [generators.patient :as patients-gen]
            [acceptance.context :refer [fx-default *driver*]]))

(defn prepare-steps [f]
  ;; Wait grid load data
  (e/wait-invisible *driver* ".//div[@class='datagrid-mask']")
  ;; Click add patient button
  (e/click *driver* {:id dg-anchors/toolbar-patient-add-button})
  ;; Wait first input ready
  (form/wait-first-field dc-anchors/dialog-form)
  (f))

(use-fixtures :each (join-fixtures [fx-default prepare-steps]))

(defn all-field-empty? [anchor]
  (= (form/get-values anchor)
         {"patient_name" ""
          "birth_date" ""
          "gender" ""
          "address" ""
          "policy_number" "____ ____ ____ ____"}))
  
(deftest empty-fields  
  (is (all-field-empty? dc-anchors/dialog-form))
  (is (link-button/disabled? dc-anchors/footer-add-button)))

(deftest without-one-field
  
  (let [{:keys [input expected]} (:ru (patients-gen/gen-fake-patient))
        input-data               (dissoc input    "patient_name")
        expected-data            (assoc  expected "patient_name" "")]

    (form/set-values dc-anchors/dialog-form input-data)
    
    (is (= (form/get-values dc-anchors/dialog-form)
           expected-data)))
    
    (is (link-button/disabled? dc-anchors/footer-add-button)))

(deftest empty-fields-after-fill-and-close

  (let [{:keys [input expected]} (:ru (patients-gen/gen-fake-patient))]
  
    (form/set-values dc-anchors/dialog-form input)
    (is (= expected
           (form/get-values dc-anchors/dialog-form))))

  (e/click *driver* {:class "panel-tool-close"})
  (e/wait-invisible *driver* {:id dc-anchors/dialog-form})

  (link-button/click dg-anchors/toolbar-patient-add-button)
  (e/wait-visible *driver* {:id dc-anchors/dialog-form})

  (is (all-field-empty? dc-anchors/dialog-form)))

(deftest positive

  (let [data (patients-gen/gen-fake-patient)]
  
    (form/set-values dc-anchors/dialog-form (-> data :ru :input))
  
    (is (link-button/enabled? dc-anchors/footer-add-button))

    (link-button/click dc-anchors/footer-add-button)
    (e/wait-invisible *driver* {:id dc-anchors/dialog-form})
    (e/wait-invisible *driver* ".//div[@class='datagrid-mask']")  

    (patients-core/data-grid-check-equal (list data))))
    
