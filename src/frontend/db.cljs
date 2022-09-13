(ns frontend.db
  (:require
   [re-frame.core :as rf]
   [frontend.patients :as p]
   [re-frame.db]))

(def locales {:en {:strings {:patinents.datagrid.column.patient-name "Patient name"}
                   :human-date-format "yyyy-MM-dd"}
              :ru {:strings {:patinents.datagrid.column.patient-name "Имя пациента"}
                   :human-date-format "dd.MM.yyyy"}})
  
(def locale (:en locales))

(def db {:locale-lang :ru
         :locale (:ru locales)
         
         :. {:patients p/init-state}})

(rf/reg-sub :locale-lang #(-> % :frontend :locale-lang))

(rf/reg-event-db :switch-local-lang
  (fn [app-state [_ lang]]                 
    (assoc-in app-state [:frontend :locale-lang] lang)))
    
(rf/reg-event-db :initialize-db
  (fn [_] {:frontend db}))

(defn state []
  (str @re-frame.db/app-db))


