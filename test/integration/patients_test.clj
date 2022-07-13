(ns integration.patients-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [etaoin.api :as e]
            [etaoin.keys :as k]
            [integration.easy-ui :as easy-ui]
            [integration.utils :as u]
            [clojure.string :as s]))

(def chrome-host (or (System/getenv "CHROME_HOST")
                     "127.0.0.1"))
(def chrome-url  (or (System/getenv "CHROME_URL")
                     "http://localhost:9009"))

(deftest dialog-create-all-fields-empty-button-disabled
  (let [driver (e/chrome {:host chrome-host})]
    (e/go driver chrome-url)
    (e/click driver [{:id :patients-datagrid-toolbar-button-add}])

    (e/wait 1)
    
    (let [patient-name (easy-ui/get-input driver "patient_name")
          birth-date (easy-ui/get-input driver "birth_date")
          gender (easy-ui/get-input driver "gender")
          address (easy-ui/get-textarea driver "address")
          policy-number (easy-ui/get-input driver "policy_number")]
      (is (and (every? s/blank? [patient-name
                                 birth-date
                                 gender
                                 address])
               (= policy-number "____ ____ ____ ____")))

      (is (contains? (u/classes driver ".//span[@id='patients-dialog_create-button-create']/a")
                     "l-btn-disabled"))
                    
      (easy-ui/fill-input driver "patient_name" "Ivan Ivanovich Ivanov")
      (easy-ui/fill-date driver "birth_date" 2992 1 2)
      (easy-ui/fill-combo driver "gender" "Мужской")
      (easy-ui/fill-textarea driver "address" "Some address")
      (easy-ui/fill-masked driver "policy_number" "1111222233334444")
      (e/click driver [{:id :patients-dialog_create-button-create}])
    
      (e/wait 1)
      (is (e/absent? driver {:id :patients-dialog_create-button-create}))
    
      ;; TODO: add asserts patients realy added;
      ;;       remove side effects (delete created patient)
    )))
