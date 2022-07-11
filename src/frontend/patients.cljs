(ns frontend.patients
  (:require
   ["rc-easyui" :refer [Layout LayoutPanel DataGrid GridColumn LinkButton Dialog Form TextBox DateBox SearchBox MaskedBox ComboBox FormField ButtonGroup]]
   [reagent.core :as r]
   [clojure.string :refer [trim replace blank?]]
   [common.patients]
   [frontend.rf-nru-nwd :refer [reg-sub]]

   [frontend.patients.datagrid :as datagrid]
   [frontend.patients.filter-panel :as filter-panel]
   [frontend.patients.dialog-create :as dialog-create]
   [frontend.patients.dialog-update :as dialog-update]
   [frontend.patients.dialog-delete :as dialog-delete]
   [frontend.comm :as comm]
   [frontend.patients.models :as models]
   [re-frame.core :as rf]))

(def init-state {:show-dialog nil
                 :show-filter-panel false
                 :. {:datagrid datagrid/init-state
                     :filter-panel filter-panel/init-state
                     :dialog-update dialog-update/init-state
                     :dialog-create dialog-create/init-state
                     :dialog-delete dialog-delete/init-state}})
                         
(reg-sub ::state #(-> %))

(rf/reg-event-db ::show-filter-panel
  (fn [state [_ v]]
    (case v
      :close (assoc state :show-filter-panel false)
      :open  (assoc state :show-filter-panel true)
      nil (assoc state :show-filter-panel
                 (not (:show-filter-panel state))))))
    
(defn ui []
  (let [local-lang @(rf/subscribe [:locale-lang])
        state @(rf/subscribe [::state])
        selection (get-in state [:. :datagrid :selection])
        {:keys [show-filter-panel]} state]
  [:> Layout {:style {"width" "100%" "height" "100%"}}

      [:> LayoutPanel {:region "north" :style {"height" "60px"}}
       [:div {:style {:text-align "center"}}
        [:h1 {:style {:margin "10px"}}
         "Patients CRUD application"]]

       [:> ButtonGroup {:selectionMode "single"
                        :style {:position "absolute"
                                :top "15px"
                                :left "10px"}}
        
        [:> LinkButton {:iconCls "en"
                        :selected (= local-lang :en)
                        :onClick #(rf/dispatch [:switch-local-lang :en])}]
        [:> LinkButton {:iconCls "ru"
                        :selected (= local-lang :ru)
                        :onClick #(rf/dispatch [:switch-local-lang :ru])}]]]
   
   
       [:> LayoutPanel {:region "west"
                        :title "Patients filters"
                        :collapsible true
                        :expander true
                        :onExpand #(rf/dispatch [::show-filter-panel :open])
                        :onCollapse #(rf/dispatch [::show-filter-panel :close])                        
                        :collapsed (not show-filter-panel)
                        :style {:width "305px"}}

        [filter-panel/entry]
        ]

;   [:> LayoutPanel {:region "south"} (str @(rf/subscribe [:patients/remote-filters]))]

   #_(let [patients-state @(rf/subscribe [:app-state])]
;         update-in patient]f
      [:> LayoutPanel {:region "east" :style {:width "300px"}} patients-state]
   )

   [:> LayoutPanel {:region "center" :style {:height "100%"}}
    [datagrid/entry state]
    [dialog-delete/entry selection]
    [dialog-create/entry models/Patient]
    [dialog-update/entry models/Patient]
                  ]
   ]))
