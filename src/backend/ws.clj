(ns backend.ws
  (:require
   [org.httpkit.server :refer [send! as-channel on-receive]]
   [schema.core :as s]
   [clojure.edn]
   [backend.ws-routes :refer [ws-request-routes]]
   [clojure.tools.logging :as log]))

(defmulti process-ws-event
  (fn [_ event-name _] event-name))

(defmethod process-ws-event :default [_ _ _]
  :event-not-found)

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
       :request-not-valid-end-string
       :details (.getMessage e)])))
      
(defn handler [ctx ring-request]
  (as-channel ring-request
     {:on-receive (fn [ch msg-str]
        (send! ch (str (ws-process-request ctx msg-str))))}))
