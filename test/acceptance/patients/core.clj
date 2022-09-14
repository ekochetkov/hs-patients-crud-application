(ns acceptance.patients.core
  (:require [backend.context :refer [ctx]]
            [backend.ws]
            [clojure.test :refer [is]]
            [etaoin.keys :as k]
            [common.ui-anchors.patients.datagrid     :as dg-anchors]
            [common.ui-anchors.patients.search-panel :as sp-anchors]
            [acceptance.easy-ui.link-button          :as link-button]
            [acceptance.easy-ui.datagrid             :as datagrid]
            [acceptance.easy-ui.form                 :as form]))

(def fn-insert-patient
  #(backend.ws/ws-process-event ctx [:patients/create %]))

(def fn-reload-datagrid
  #(link-button/click dg-anchors/toolbar-reload-page))

(def fn-wait-datagrid-data-changed
  (partial datagrid/wait-datagrid-data-changed dg-anchors/datagrid-table))

(def fn-apply-filter
  (fn [form-data]
    (form/set-values dg-anchors/toolbar-filter-box form-data)))

(def fn-apply-search
  (fn [form-data]
    (form/set-values sp-anchors/form form-data)
    (if (= (-> form-data vals first)
           k/backspace)
      (link-button/click sp-anchors/reset-button)
      (link-button/click sp-anchors/apply-button))))

(def fn-precoditions
  (fn [pre-rows]
     (doall (->> pre-rows
                 (map :db pre-rows)
                 (map fn-insert-patient)))
    
    (fn-reload-datagrid)

    (let [all-actual-rows          (fn-wait-datagrid-data-changed)
          all-actual-rows-expected (map #(-> % :ru :expected) pre-rows)]

    (is (= (set all-actual-rows-expected)
           (set all-actual-rows))))))

(defn data-grid-check-equal [expected-rows & [last-rows-actual]]
  (let [all-actual-rows          (fn-wait-datagrid-data-changed last-rows-actual)
        all-actual-rows-expected (map #(-> % :ru :expected) expected-rows)]
    
    (is (= (count all-actual-rows-expected)
           (count all-actual-rows)))
    
    (is (= (set all-actual-rows-expected)
           (set all-actual-rows)))))

(defn precondition-datagrid [rows]
  (doall (->> rows
              (map :db rows)
              (map fn-insert-patient)))
  (fn-reload-datagrid)
  (data-grid-check-equal rows))
 
