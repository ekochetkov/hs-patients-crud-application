(ns frontend.patients
  (:require
   ["rc-easyui" :refer [Layout LayoutPanel DataGrid GridColumn LinkButton Dialog Form TextBox DateBox SearchBox MaskedBox ComboBox FormField ButtonGroup]]
   [reagent.core :as r]
   [clojure.string :refer [trim replace blank?]]
   [common.patients]
   [frontend.rf-nru-nwd :refer [reg-sub]]

   [frontend.patients.datagrid :as datagrid]
;   [frontend.patients.filter-panel :as filter-panel]
   [frontend.patients.dialog-create :as dialog-create]
   [frontend.patients.dialog-update :as dialog-update]
   [frontend.patients.dialog-delete :as dialog-delete]
   [frontend.comm :as comm]
   [frontend.patients.models :as models]
   [re-frame.core :as rf]))

(def init-state {:show-dialog nil
                 :. {:datagrid datagrid/init-state
;;                     :filter-panel filter-panel/init-state
                     :dialog-update dialog-update/init-state
                     :dialog-create dialog-create/init-state
                     :dialog-delete dialog-delete/init-state
                     }})
                         
(reg-sub ::state #(-> %))

(rf/reg-event-fx ::datagrid-reload
  (fn [cofx]
    (assoc cofx :fx
       [[:dispatch [::datagrid/start-loading]]
        [:dispatch [::comm/send-event ::datagrid/read [:patients/read {} 0 0]]]])))

(defn ui []
  (let [state @(rf/subscribe [::state])
        selection (get-in state [:. :datagrid :selection])]
  [:> Layout {:style {"width" "100%" "height" "100%"}}

       #_[:> LayoutPanel {:region "west"
                        :title "Patients filters"
                        :collapsible true
                        :expander true
                        :style {:width "300px"}}

;        [filter-layout-panel-content]
        ]

;   [:> LayoutPanel {:region "south"} (str @(rf/subscribe [:patients/remote-filters]))]

   #_(let [patients-state @(rf/subscribe [:app-state])]
;         update-in patient]f
      [:> LayoutPanel {:region "east" :style {:width "300px"}} patients-state]
   )

   [:> LayoutPanel {:region "center" :style {:height "100%"}}
    [datagrid/entry]
    [dialog-delete/entry selection]
    [dialog-create/entry models/Patient]
    [dialog-update/entry models/Patient]
                  ]
   ]))
