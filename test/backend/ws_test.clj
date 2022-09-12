(ns backend.ws-test
  (:require
   [backend.ws :as ws :refer [ws-process-event]]
   [clojure.test :refer [deftest is]]))

(defmethod ws-process-event :echo
  [_ [_ & event-args]] event-args)

(deftest positive-event-echo
  (let [sys (atom {})
        event (into [:echo]
                    (repeat (inc (rand-int 5)) (java.util.UUID/randomUUID)))]
    (is (= (rest event)
           (ws-process-event sys event)))))

(deftest positive-echo-test
  (let [ctx {}
        event (into [:echo]
                    (repeat (inc (rand-int 5)) (java.util.UUID/randomUUID)))
        request-uuid (java.util.UUID/randomUUID)
        request {:proto :request
                 :id request-uuid
                 :data event}
        response (ws/ws-process-request ctx (str request))]

    (is (= request response))))

(defmethod ws-process-event :echo-one-value
  [_ [_ value]] value)

(deftest positive-echo-one-value-hash-map-test
  (let [ctx {}
        data {:a 1 :b 2}
        event [:echo-one-value data]
        request-uuid (java.util.UUID/randomUUID)
        request {:proto :request
                 :id request-uuid
                 :data event}
        response (ws/ws-process-request ctx (str request))]
    (is (= request response))))

(deftest positive-echo-one-value-scalar
  (let [ctx {}
        data (rand-int 100)
        event [:echo-one-value data]
        request-uuid (java.util.UUID/randomUUID)
        request {:proto :request
                 :id request-uuid
                 :data event}
        response (ws/ws-process-request ctx (str request))]
    (is (= request response))))
    
(deftest negative-not-valid-edn-message
  (let [ctx {}
        message-str "[}"
        response (ws/ws-process-request ctx message-str)]

    (is (= ((juxt first
                  #(-> % second :code)) response)
           [:comm/error :request-not-valid-edn-string]))))

(deftest negative-incorrect-request-base-keys-schema
  (let [ctx {}
        request {:protox :request
                 :idx (java.util.UUID/randomUUID)
                 :data [:echo]}        
        response (ws/ws-process-request ctx (str request))]

    (is (= ((juxt first
                  #(-> % second :code)) response)
           [:comm/error :request-schema-not-valid]))))

(deftest negative-incorrect-request-content-schema
  (let [ctx {}
        request {:proto :request
                 :id (java.util.UUID/randomUUID)
                 :data [":echo"]}        
        response (ws/ws-process-request ctx (str request))]

    (is (= ((juxt :proto :id) request)
           ((juxt :proto :id) response)))

    (is (= ((juxt #(-> % :data first)
                  #(-> % :data second :code)) response)
           [:comm/error :request-schema-not-valid]))))
    
(defmethod ws-process-event :event-with-exception
  [_] (throw (Exception. "Some exception in event")))

(deftest negative-exception-on-process
  (let [ctx {}
        request {:proto :request
                 :id (java.util.UUID/randomUUID)
                 :data [:event-with-exception]}        
        response (ws/ws-process-request ctx (str request))]
    
    (is (= (-> response :data first) :comm/error))
    (is (= (-> response :data second :code) :exception-process-request))))
