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

(def db {
         :. {:layout l/init-state
             :patients p/init-state}
         })

;(def app-db-default
;  (assoc db :module.patients patients/db))

(rf/reg-event-db :initialize-db
  (fn [_] {:frontend db}))

(defn state []
  (js/console.log "state" (str @re-frame.db/app-db)))
