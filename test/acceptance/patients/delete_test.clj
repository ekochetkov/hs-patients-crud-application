(ns acceptance.patients.delete-test
  (:require [clojure.test :refer [deftest use-fixtures join-fixtures]]
            [etaoin.api :as e]
            [backend.ws-events]
            [common.ui-anchors.patients.dialog-delete :as dd-anchors]                  
            [common.ui-anchors.patients.datagrid :as dg-anchors]
            [acceptance.patients.core :as patients-core]
            [generators.patient :as patients-gen]
            [acceptance.easy-ui.link-button :as link-button]
            [acceptance.easy-ui.datagrid :as datagrid]
            [acceptance.context :refer [fx-default *driver*]]))
  
(defn prepare-steps [f]
  ;; Wait grid load data
  (e/wait-invisible *driver* ".//div[@class='datagrid-mask']")
  (f))

(use-fixtures :each (join-fixtures [fx-default prepare-steps]))

;;(def driver (e/firefox))
;;(e/go driver "http://localhost:8080/shadow")

(deftest patients-delete-positive-test
  (let [data-insert (patients-gen/gen-unique-fake-patients 3)]
;;(e/wait 8)
    ;; Pre
    (patients-core/precondition-datagrid data-insert)

    ;; Action
    (let [delete-n (rand-int (count data-insert))
          actual-rows (vec (patients-core/fn-wait-datagrid-data-changed))
          row-for-delete (get actual-rows delete-n)]

      (assert (not (nil? row-for-delete)))
      
      (datagrid/context-menu dg-anchors/datagrid-table
                             delete-n
                             "//div[@class='menu-icon icon-cancel']")
      
      (link-button/click dd-anchors/button-conform)
      (e/wait-invisible *driver* {:id dd-anchors/dialog})

    ;; Post
      (patients-core/data-grid-check-equal
              (doall (filter #(not= (-> % :ru :expected) row-for-delete) data-insert))
                                         data-insert)))
;;  (e/wait 5)
  )

(deftest patients-delete-cancel-test
  (let [data-exists (patients-gen/gen-unique-fake-patients 10)]

    ;; Pre
    (patients-core/precondition-datagrid data-exists)

    ;; Action
    (let [delete-n (rand-int (count data-exists))]

      (datagrid/context-menu dg-anchors/datagrid-table
                             delete-n
                             "//div[@class='menu-icon icon-cancel']")
      (link-button/click dd-anchors/button-cancel)
      (e/wait-invisible *driver* {:id dd-anchors/dialog})

    ;; Post
    (patients-core/data-grid-check-equal data-exists))))
