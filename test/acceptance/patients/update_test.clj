(ns acceptance.patients.update-test
  (:require [clojure.test :refer [deftest use-fixtures join-fixtures is]]
            [etaoin.api :as e]
            [backend.ws-events]
            [acceptance.patients.core :as patients-core]
            [acceptance.easy-ui.form :as form]
            [acceptance.easy-ui.link-button :as link-button]
            [acceptance.easy-ui.datagrid :as datagrid]
            [acceptance.utils :as u]                                    
            [common.ui-anchors.patients.dialog-update :as du-anchors]            
            [common.ui-anchors.patients.datagrid :as dg-anchors]
            [generators.patient :as patients-gen]
            [acceptance.context :refer [fx-default *driver*]]))

;;(def driver (e/firefox))
;;(e/go driver "http://localhost:8080/shadow")

(defn prepare-steps [f]
  ;; Wait grid load data
  (patients-core/fn-wait-datagrid-data-changed)
  (f))

(use-fixtures :each (join-fixtures [fx-default prepare-steps]))

(deftest patients-update-positive-test
  (let [unique-data (patients-gen/gen-unique-fake-patients 10)
        data-exists (drop-last unique-data)
        data-update (last unique-data)]

    ;; Pre
    (patients-core/precondition-datagrid data-exists)

    ;; Action
    (let [update-n (rand-int (count data-exists))
          actual-rows (vec (patients-core/fn-wait-datagrid-data-changed))
          row-for-update (get actual-rows update-n) 
          update-n-exists (u/index-of #(= (-> % :ru :expected)
                                          row-for-update) data-exists)]

      (datagrid/context-menu dg-anchors/datagrid-table
                             update-n
                             "//div[@class='menu-icon icon-edit']")
      
      (form/set-values du-anchors/dialog-form (-> data-update :ru :input))
      (e/wait-predicate #(link-button/enabled? du-anchors/footer-save-button))
      (link-button/click du-anchors/footer-save-button)
      (e/wait-invisible *driver* {:id du-anchors/dialog-form})
      
      ;; Post
      (is (patients-core/data-grid-check-equal (u/replace-nth update-n-exists
                                                          data-exists
                                                          data-update) data-exists)))))

(deftest patients-update-double-test
  (let [unique-data (patients-gen/gen-unique-fake-patients 10)
        data-exists (drop-last unique-data)
        data-update (assoc-in (last unique-data)
                              [:ru :input "policy_number"] (get-in (first data-exists)
                                                                   [:ru :input "policy_number"]))]

    ;; Pre
    (patients-core/precondition-datagrid data-exists)

    ;; Action
    (let [update-n (rand-int (count data-exists))]

      (datagrid/context-menu dg-anchors/datagrid-table
                             update-n
                             "//div[@class='menu-icon icon-edit']")
      
      (form/set-values du-anchors/dialog-form (-> data-update :ru :input))
      (e/wait-predicate #(link-button/enabled? du-anchors/footer-save-button))
      
      (link-button/click du-anchors/footer-save-button)
      
      (e/wait-predicate #(link-button/disabled? du-anchors/footer-save-button)))))

(deftest patients-update-cancel-save
  (let [unique-data (patients-gen/gen-unique-fake-patients 10)
        data-exists (drop-last unique-data)
        data-update (last unique-data)]

    ;; Pre
    (patients-core/precondition-datagrid data-exists)

    ;; Action
    (let [update-n (rand-int (count data-exists))]

      (datagrid/context-menu dg-anchors/datagrid-table
                             update-n
                             "//div[@class='menu-icon icon-edit']")
    
      (form/set-values du-anchors/dialog-form (-> data-update :ru :input))                 
      (e/click *driver* {:class "panel-tool-close"})
      (e/wait-invisible *driver* {:id du-anchors/dialog-form})
  
      ;; Post
      (patients-core/data-grid-check-equal data-exists))))
