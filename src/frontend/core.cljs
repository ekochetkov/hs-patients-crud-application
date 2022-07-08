(ns frontend.core
  (:require
   ["rc-easyui" :refer [DataGrid GridColumn Layout LayoutPanel Tree LinkButton Dialog Form TextBox Label DateBox]]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [re-frame.core :as rf]
   [frontend.patients.datagrid :as dg]
   [frontend.db]
   [frontend.layout :as layout]
   [frontend.comm :as comm]
   [frontend.patients :as p]
   [frontend.rf-nru-nwd :as rf-nru-nwd]
   ))

(def locales {:en {:human-date-format "yyyy-MM-dd"}
              :ru {:human-date-format "dd.MM.yyyy"}})

(def locale (:en locales))

(rf/reg-global-interceptor (rf-nru-nwd/interceptor :frontend))

(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el) 
    (rdom/render #_[dg/entry] [p/ui] root-el)))

(defn main []
  (js/console.log "main") 
  (rf/dispatch-sync [:initialize-db])
  (comm/start js/WS_URL)
  (rf/dispatch [::p/datagrid-reload])
  (mount-root))

(js/console.log "App entry point here" js/WS_URL)
(main)
