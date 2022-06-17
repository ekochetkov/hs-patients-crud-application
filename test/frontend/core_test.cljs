(ns frontend.core-test
  (:require
   [frontend.core]
   [day8.re-frame.test :as rf-test]))

;(deftest init
;  (rf-test/run-test-sync       
;    (rf/dispatch [:initialize-db])   
;    (let [sa (rf/subscribe [:server-answer])]
;      (is (= "---" @sa)))))
