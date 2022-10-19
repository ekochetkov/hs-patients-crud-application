(ns acceptance.patients.00-default-view-test
  (:require [clojure.test :refer [deftest is use-fixtures join-fixtures]]
            [etaoin.api :as e]
            [backend.ws-events]
            [acceptance.context :refer [fx-default *driver*]]
            [acceptance.utils :refer [md5]])
  (:import [java.io File]))
  
(defn prepare-steps [f]
  (e/wait-invisible *driver*
                    ".//div[@class='datagrid-mask']")
  (f))

(use-fixtures :each (join-fixtures [fx-default prepare-steps]))

(deftest default-view
  ;; Screenshot test only for Firefox
  (if (= (get-in *driver* [:capabilities
                             :desiredCapabilities
                             :browserName])
           "firefox")
    (let [file (File/createTempFile "default-view-test-" ".png")]  
        (e/screenshot *driver* file)
        (is (= (md5 (slurp "./test/acceptance/patients/screenshots/default-view.png"))
               (md5 (slurp file)))))
    (println "Skip test for non-firefox browsers")))
