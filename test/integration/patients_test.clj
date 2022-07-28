(ns integration.patients-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [etaoin.api :as e]
            [etaoin.keys :asxo k]
            [integration.easy-ui :as easy-ui]
            [integration.utils :as u]
            [etaoin.impl.util :as util :refer [defmethods]]
            [clojure.string :as s]))

(def selenium-grid-hub-url (System/getenv "SELENIUM_GRID_HUB_URL"))
(def selenium-grid-browser (System/getenv "SELENIUM_GRID_BROWSER"))
(def app-url (System/getenv "APP_URL"))

(when (nil? selenium-grid-hub-url)
  (throw (Exception. "Need env var 'SELENIUM_GRID_HUB_URL'")))

(when (nil? selenium-grid-browser)
  (throw (Exception. "Need env var 'SELENIUM_GRID_BROWSER'")))

(when (nil? app-url)
  (throw (Exception. "Need env var 'APP_URL'")))

(def driver-opts {:webdriver-url selenium-grid-hub-url
                  :raw-capabilities {
                  :capabilities {:firstMatch [{}]
                                 :alwaysMatch {:browserName selenium-grid-browser}}
                  :desiredCapabilities {:browserName selenium-grid-browser}}})

(deftest ^:integration dialog-create-all-fields-empty-button-disabled
  (println "run test" selenium-grid-hub-url app-url)
  (e/with-selenium-grid driver-opts driver
    (e/go driver app-url)

    (e/wait-invisible driver ".//div[@class='datagrid-mask']")
    
    (e/click driver [{:id :patients-datagrid-toolbar-button-add}])

    (e/wait 2)
    
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
    
      (e/wait 5)
      (is (e/absent? driver :patients-dialog_create-button-create))
    
      ;; TODO: add asserts patients realy added;
      ;;       remove side effects (delete created patient)
    )))
