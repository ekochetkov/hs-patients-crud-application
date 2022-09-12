(ns backend.ws
  (:require
   [org.httpkit.server :refer [send! as-channel]]
   [schema.core :as s]
   [backend.rop :refer [=>>]]
   [clojure.edn]
   [clojure.tools.logging :as log]))

(defmulti ws-process-event
  (fn [_ [event-name & _]] event-name))

(defmethod ws-process-event :default [_ [event-name & _]]
  (throw (Exception. (str "Event: '" event-name "' not found"))))

(defn parse-edn-string [msg-str]
  (try
    [(clojure.edn/read-string msg-str) nil]
    (catch Exception e
      [nil [:comm/error {:code :request-not-valid-edn-string
                         :message (.getMessage e)}]])))

(defn validate-request-schema [message]
  (let [event-schema   [(s/one s/Keyword "keyword") s/Any]
        request-schema {(s/required-key :proto) s/Keyword
                        (s/required-key :id)    java.util.UUID
                        (s/required-key :data)  event-schema}]
    (try
      (s/validate request-schema message)
      [message nil]
    (catch Exception e
      (let [error-info [:comm/error {:code    :request-schema-not-valid
                                     :message (.getMessage e)}]]
        [nil (if (and (:proto message) (:id message))
               (assoc message :data error-info)
               error-info)])))))

(defn process-event-and-build-response [sys message]
  (let [event (:data message)
        event-name (first event)]
    (try
      (let [result (ws-process-event sys event)
            data (if (sequential? result)
                   (into [event-name] result)
                   [event-name result])]
      [(assoc message :data data) nil])
    (catch Exception e
      [nil (assoc message :data [:comm/error {:code :exception-process-request
                                              :message (.getMessage e)}])]))))

(defn ws-process-request [sys msg-str]
  (let [[result error] (=>> msg-str
                            parse-edn-string
                            validate-request-schema
                            (partial process-event-and-build-response sys))]
    (or error result)))

(defn handler [ctx ring-request]
  (as-channel ring-request
     {:on-receive (fn [ch msg-str]
        (send! ch (str (ws-process-request ctx msg-str))))
      :on-open (fn [ch]
        (log/info (str "New WS-channel open: " ch)))}))
