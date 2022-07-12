(ns integration.patients-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [etaoin.api :as e]))

(def driver (e/firefox))

(e/go driver "http://localhost:9009")

(e/wait-visible driver [{:id :patients-datagrid-toolbar-button-add}])

(e/get-element-text driver {:id :patients-datagrid-toolbar-button-add})

(e/click driver [{:id :patients-datagrid-toolbar-button-add}])

(e/click driver [{:id :patients-dialog_create-button-create}])

(e/click driver [{:id :patient_name}])

(e/fill driver ".//span[@id='patient_name']//input" "Ivan Ivanovich Ivanov")

(e/fill driver ".//span[@id='patient_name']//input" "Ivan Ivanovich Ivanov")
