(ns frontend.rf-nru-nwd
  (:require
   [re-frame.core]
   [re-frame.interceptor :refer [->interceptor get-effect get-coeffect assoc-coeffect assoc-effect update-coeffect]]
   [clojure.string :as s]))

(defn id->path [id]
  (let [ns-names (s/split (namespace id) #"\.")]
    (drop-last (flatten (map #(vector (keyword %) :.) ns-names)))))

(defn interceptor [use-in-ns]
  (let [db-store-key :re-frame-path/db-store]
    (->interceptor
      :id      :path
      :before  (fn
                 [context]
                 (let [event (first (get-coeffect context :event))
                       path (id->path event)
                       event-ns (first path)
                       original-db (get-coeffect context :db)]
                   (if (= use-in-ns event-ns)                   
                     (-> context
                       (update db-store-key conj original-db)
                       (assoc-coeffect :db (get-in original-db path)))
                     context
                   )))
      :after   (fn [context]
                 (let [event (first (get-coeffect context :event))
                       path (id->path event)
                       event-ns (first path)                       
                       db-store     (db-store-key context)
                       original-db  (peek db-store)
                       new-db-store (pop db-store)
                       context'     (-> (assoc context db-store-key new-db-store)
                                        (assoc-coeffect :db original-db))
                       db           (get-effect context :db ::not-found)]
                   (if (= use-in-ns event-ns)                                      
                     (if (= db ::not-found)
                       context'
                       (->> (assoc-in original-db path db)
                            (assoc-effect context' :db)))
                     context))))))

(defn reg-sub [id func]
  (let [path (id->path id)]
    (re-frame.core/reg-sub id
      (fn [app-state] (func (get-in app-state path))))))
                           
