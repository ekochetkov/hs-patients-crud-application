(ns backend.ws
  (:require
   [org.httpkit.server :refer [send! as-channel]]
   [schema.core :as s]
   [clojure.edn]
   [backend.ws-events :refer [process-ws-event]]
   [backend.patients-events]
   [clojure.tools.logging :as log]))

(defmethod process-ws-event :default [_ _ _]
  (throw (Exception. "Event not found")))

(defn ws-process-request [ctx msg-str]
  (try
    (let [message (clojure.edn/read-string msg-str)
          event (:data message)
          event-name (first event)
          event-args (rest event)]
          (s/validate [(s/one s/Keyword "keyword") s/Any] event)
      (assoc message :data    
        (try 
          [event-name (process-ws-event ctx event-name event-args)]
        (catch Exception e
          [:comm/error
           :exception-process-request
           :details (.getMessage e)]))))
    (catch Exception e
      [:comm/error
       :request-not-valid-edn-string
       :details (.getMessage e)])))
      
(defn handler [ctx ring-request]
  (as-channel ring-request
     {:on-receive (fn [ch msg-str]
        (send! ch (str (ws-process-request ctx msg-str))))
      :on-open (fn [ch]
        (log/info (str "New WS-channel open: " ch)))}))
