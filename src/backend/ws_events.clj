(ns backend.ws-events)

(defmulti process-ws-event
  (fn [_ event-name _] event-name))
