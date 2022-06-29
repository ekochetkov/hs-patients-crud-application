(ns backend.ws
  (:require
   [org.httpkit.server :refer [send! with-channel on-close on-receive]]
   [schema.core :as s]
   [clojure.edn]
   [backend.ws-routes :refer [ws-request-routes]]
   [clojure.tools.logging :as log]
   ))

(defonce channels (atom #{}))

;(defonce ws-request-schema
;  [(

(defonce f-args-schema
  [(s/one s/Keyword "keyword") s/Any])

(defn connect! [channel]
 ;(log/info "channel open")
  (swap! channels conj channel)
  (println channels))

(defn disconnect! [channel status]
 ;(log/info "channel closed:" status)
 (swap! channels #(remove #{channel} %)))

(defn process-request [ctx request ws-routes]
  (assoc request :data  
    (try
      (let [data (:data request)
            func (first data)
            args (rest data)]
         (s/validate [(s/one s/Keyword "keyword") s/Any] data)
         (if-let [f (get ws-routes func)]
           [func (apply (partial f ctx) args)]
           [:comm/error :not-exists-func]))
      (catch Exception e
        [:comm/error :exception-process-request
         :details (.getMessage e)]))))

(defn ws-on-receive [ctx channel message-str]
                                        ;(log/info (format "New message from channel %s %s" channel message-str))
  (try
    (let [request (clojure.edn/read-string message-str)
          result  (process-request ctx request ws-request-routes)
          response (str result)]
    (log/info (format "Request: >%s< response: >%s<" message-str response))
    (send! channel response))
  (catch Exception e
    (log/info (str "Exception on receive request: " (.getMessage e)))
    (send! channel "Requst not in re-frame websocken-fx edn format!"))))

(defn handler [ctx request]
  (with-channel request channel
    (connect! channel)
    (on-close channel (partial disconnect! channel))
    (on-receive channel (partial ws-on-receive ctx channel))))

;(clojre.edn/read-string "")

;;(s/validate [(s/one s/Keyword :func-name) s/Any] [3])


;; process-request "{:data [:add 2 2]}"   [(schema/one schema/Keyword "keyword") (schema/optional s] {})


