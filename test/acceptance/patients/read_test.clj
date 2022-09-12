(ns acceptance.patients.read-test
  (:require [clojure.test :refer [deftest is use-fixtures join-fixtures]]
            [etaoin.api :as e]
            [backend.ws-events]
            [common.ui-anchors.patients.datagrid :as dg-anchors]
            [acceptance.easy-ui.datagrid :as datagrid]
            [generators.patient :as patients-gen]
            [acceptance.patients.core :as patients-core]            
            [acceptance.context :refer [fx-default *driver*]]))
  
(defn prepare-steps [f]
  ;; Wait grid load data
  (e/wait-invisible *driver* ".//div[@class='datagrid-mask']")
  (f))

(use-fixtures :each (join-fixtures [fx-default prepare-steps]))

(deftest patients-datagrid-empty-after-start-test
  (patients-core/data-grid-check-equal '()))

(deftest patients-datagrid-paginator-test
  
  (let [data-exists (patients-gen/gen-unique-fake-patients 80)]

    (doall (->> data-exists
                 (map (comp patients-core/fn-insert-patient :db))))

  (patients-core/fn-reload-datagrid)

  (let [pages-info (datagrid/get-data-by-pages dg-anchors/datagrid-table)
        
        all-paginator-info (map (fn [[_ v]] (:paginator-info v)) pages-info)
        all-items          (mapcat (fn [[_ v]] (:items v)) pages-info)]

    (is (= '("Показаны записи с 1 по 35. Всего 80."
             "Показаны записи с 36 по 70. Всего 80."
             "Показаны записи с 71 по 80. Всего 80.")
           all-paginator-info))

    (is (= (set (doall (map #(-> % :ru :expected) data-exists)))
           (set all-items))))))
