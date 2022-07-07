(ns frontend.core
  (:require
   ["rc-easyui" :refer [DataGrid GridColumn Layout LayoutPanel Tree LinkButton Dialog Form TextBox Label DateBox]]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [re-frame.core :as rf]

   [frontend.db]
   [frontend.layout :as layout]
   [frontend.comm :as comm]
   [frontend.patients]
   [frontend.rf-nru-nwd :as rf-nru-nwd]
   ))

(rf/reg-global-interceptor (rf-nru-nwd/interceptor :frontend))

(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el) 
    (rdom/render [layout/ui] root-el)))

(defn main []
  (js/console.log "main") 
  (rf/dispatch-sync [:initialize-db])
;  (comm/start js/WS_URL)
;  (rf/dispatch [:patients/patients-reload])
  (mount-root))

(js/console.log "App entry point here" js/WS_URL)
(main)
