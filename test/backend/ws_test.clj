(ns backend.ws-test
  (:require
   [backend.ws :as ws]
   [backend.ws-events :refer [process-ws-event]]
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

(deftest negative-not-valid-end-message
  (let [ctx {}
        message-str "not valid edn string"
        result (ws/ws-process-request ctx message-str)]
    (is (= (first  result) :comm/error))
    (is (= (second result) :request-not-valid-edn-string))))

(defmethod process-ws-event :event-with-exctiption
  [_ _ _] (throw (Exception. "Some exception in event")))

(deftest negative-exception-on-process
  (let [ctx {}
        request {:data [:event-with-exctiption]}
        result (ws/ws-process-request ctx (str request))]
    (is (= (-> result :data first) :comm/error))
    (is (= (-> result :data second) :exception-process-request))))
