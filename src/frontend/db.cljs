(ns frontend.db
  (:require
   ["rc-easyui" :refer [DataGrid GridColumn Layout LayoutPanel Tree LinkButton Dialog Form TextBox Label DateBox]]
   [reagent.core :as r]
   [reagent.dom :as rdom]   
   [re-frame.core :as rf]
   [goog.string :as gstring]
   [websocket-fx.core :as wfx]
   [frontend.modules :as m]
   [frontend.patients :as p]
   [frontend.layout :as l]
   [re-frame.db]
   ))

(def locales {:en {:strings {:patinents.datagrid.column.patient-name "Patient name"}
                   :human-date-format "yyyy-MM-dd"}
              :ru {:strings {:patinents.datagrid.column.patient-name "Имя пациента"}
                   :human-date-format "dd.MM.yyyy"}})
  

(def locale (:en locales))

(def db {:locale-lang :en
         :locale (:ru locales)
         
         :. {:layout l/init-state
             :patients p/init-state}
         })

;(def app-db-default
                                        ;  (assoc db :module.patients patients/db))

(rf/reg-sub :locale-lang #(-> % :frontend :locale-lang))

(rf/reg-event-db :switch-local-lang
  (fn [app-state [_ lang]]                 
    (assoc-in app-state [:frontend :locale-lang] lang)))
    
(rf/reg-event-db :initialize-db
  (fn [_] {:frontend db}))

(defn state []
  (js/console.log "state" (str @re-frame.db/app-db)))


