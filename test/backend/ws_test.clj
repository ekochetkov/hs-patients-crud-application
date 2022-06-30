(ns backend.ws-test
  (:require
    [backend.ws :as ws :refer [process-ws-event]]
    [clojure.test :refer [deftest is]]))

(defmethod process-ws-event :echo
  [_ _ event-args] event-args)

(deftest positive-echo-test
  (let [ctx {}
        event [:echo (repeat (rand-int 5) (java.util.UUID/randomUUID))]
        request-uuid (java.util.UUID/randomUUID)
        request {:proto :request
                 :id request-uuid
                 :data event}
        response (ws/ws-process-request ctx (str request))]    
    (is (= (:proto response)
           (:proto request)))
    (is (= (:id response)
           (:id request)))
    (is (= (-> response :data second)
           (-> request :data rest)))))




        
