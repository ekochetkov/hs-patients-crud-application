(ns frontend.db
  (:require
   ["rc-easyui" :refer [DataGrid GridColumn Layout LayoutPanel Tree LinkButton Dialog Form TextBox Label DateBox]]
   [reagent.core :as r]
   [reagent.dom :as rdom]   
   [re-frame.core :as rf]
   [goog.string :as gstring]
   [websocket-fx.core :as wfx]

   [frontend.patients :as patients]
   ))

(def db {:center "patients"})

(def app-db-default
  (merge db patients/db))

(rf/reg-event-db :initialize-db
  (fn [_ _] app-db-default))
