(ns frontend.rf-isolate-ns
  (:require
   [re-frame.core]
   [clojure.string :as s]))

(defn isolate-change-namespace-state-db [func]
  (fn [app-state event]       
    (let [event-name (first event)
          event-namespace (namespace event-name)
;          namespace-state-path (s/split event-namespace #"\.")
          ns-state-current (get app-state event-namespace)
          ns-state-next (func ns-state-current event)]
      (js/console.log "xxx rg ev db isolate"  event-namespace (str ns-state-next) "!!!" )      
      (assoc app-state event-namespace ns-state-next))))
  
(defn reg-event-db [event-name func] 
  (js/console.log "xxx rg ev db" (str event-name))
  (re-frame.core/reg-event-db event-name
    (partial (isolate-change-namespace-state-db func))))                              

(defn isolate-change-namespace-state-fx [func]
  (fn [cofx event]       
    (let [event-name (first event)
          event-namespace (namespace event-name)
;          namespace-state-path (s/split event-namespace #"\.")
          ns-state-current (get-in cofx [:db event-namespace])
          ns-cofx-next (func ns-state-current event)
          ns-next-db (:db ns-cofx-next)
          ns-next-fx (:fx ns-cofx-next)]
      (js/console.log "xxx rg ev fx isolate"  event-namespace (str ns-next-db) "!!!" )
      (-> cofx
          (assoc-in [:db event-namespace] ns-next-db)
          (assoc :fx ns-next-fx)))))

(defn reg-event-fx [event-name func]
  (js/console.log "xxx rg ev fx" (str event-name))
  (re-frame.core/reg-event-fx event-name
    (partial (isolate-change-namespace-state-fx func))))

;(reg-event-fx              ;; -fx registration, not -db registration
;  :my-event
;  (fn [cofx [_ a]]        ;; 1st argument is coeffects, instead of db
;    {:db       (assoc (:db cofx) :flag  a)
;     :fx       [[:dispatch [:do-something-else 3]]]})) ;; return effects
