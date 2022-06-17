(ns frontend.patients
  (:require
   ["rc-easyui" :refer [DataGrid GridColumn Layout LayoutPanel Tree LinkButton Dialog Form TextBox Label DateBox SearchBox MaskedBox ComboBox FormField]]
   [reagent.core :as r]
   [reagent.dom :as rdom]   
   [re-frame.core :as rf]
   [goog.string :as gstring]

   [frontend.comm :as comm]
   
   [websocket-fx.core :as wfx]))

(def db {:patients {:data []
                    :data-filtered []
                    :data-filters {:text-like ""}                    
                    :show-dialog nil
                    :form-data-invalid true
                    :form-data {}
                    :form-errors {}}})

(rf/reg-sub :patients/form-data-invalid
  #(get-in % [:patients :form-data-invalid]))

(rf/reg-sub :patients/show-dialog
  #(get-in % [:patients :show-dialog]))

(rf/reg-sub :patients/data-filtered
  #(clj->js (get-in % [:patients :data-filtered])))

(rf/reg-sub :patients/form-data
  #(get-in % [:patients :form-data]))

(rf/reg-sub :patients/data-filters
  #(get-in % [:patients :data-filters]))

(rf/reg-event-db :patients/show-dialog
  (fn [app-db [_ dialog]]
    (assoc-in app-db [:patients :show-dialog] dialog)))

(rf/reg-event-db :patients/update-form-data
  (fn [app-state [_ [field value]]]
;    (js/console.log field value (type value))
    (assoc-in app-state [:patients :form-data field] value)))

(rf/reg-event-db :patients/form-data-on-validate
  (fn [app-state [_ errors]]
;    (js/console.log (str "onValidate" (.stringify js/JSON errors)))
    (if errors
      (assoc-in app-state [:patients :form-data-invalid] true)
      (assoc-in app-state [:patients :form-data-invalid] false))))

(rf/reg-event-fx :patients/clear-data-filtered
(fn [cofx _]
  (-> cofx
      (assoc-in [:db :patients :data-filtered] [])
      (assoc :dispatch [:patients/filter-data]))))

(rf/reg-event-fx :patients/update-data-filters   
  (fn [cofx [_ [field value]]]
    (-> cofx
        (assoc-in [:db :patients :data-filters field] value)
        (assoc :dispatch [:patients/clear-data-filtered]))))  

(rf/reg-event-db :patients/filter-data
  (fn [app-state _]
    (let [data (get-in app-state [:patients :data])]
      (if-let [text-like (get-in app-state [:patients :data-filters :text-like])]
        (assoc-in app-state [:patients :data-filtered]
                  (filter (fn [row] (some #(re-find (js/RegExp. text-like "i") (str %)) (vals (:resource row)))) data))
        (assoc-in app-state [:patients :data-filtered] data)))))

(defn timestamp->human-date [ts]
  (let [date (new js/Date ts)
        y (.getFullYear date)
        m (inc (.getMonth date))
        d (.getDate date)]
    (str y "-" (when (< m 10) "0") m  
           "-" (when (< d 10) "0") d )))

(rf/reg-event-fx :patients/data
  (fn [cofx [_ data]]
    (let [rendered-data (mapv (fn [row rowIndex]
                                (-> row
                                 (assoc "#" rowIndex)
                                 (assoc-in [:resource "birth_date"] (timestamp->human-date (get-in row [:resource "birth_date"])))))
                              data (range 1 (inc (count data))))]
    (-> cofx
        (assoc-in [:db :patients :data] rendered-data)
        (assoc :dispatch [:patients/clear-data-filtered])))))

(rf/reg-event-fx :patients/patients-reload
  (fn [cofx _]
    (assoc cofx :dispatch [:comm/send-event [:patients/data {} 0 100]])))

(rf/reg-event-fx :patients/send-event-create
  (fn [cofx _]
    (let [data (get-in cofx [:db :patients :form-data])]
        (assoc cofx :dispatch [:comm/send-event [:patients/create data]]))))

(rf/reg-event-fx :patients/create
  (fn [cofx _]
    (-> cofx
      (assoc-in [:db :patients :show-dialog] nil)
      (assoc-in [:db :patients :form-data] {})
      (assoc :dispatch [:patients/patients-reload]))))

(defn datagrid-toolbar []
  (let [filters (rf/subscribe [:patients/data-filters])]
  (r/as-element [:div
                 [:> LinkButton {:style {:margin "5px"}
                                 :iconCls "icon-add"
                                 :onClick #(rf/dispatch [:patients/show-dialog :create]) } "Add"]
                 [:> LinkButton {:style {:margin "5px"}
                                 :iconCls "icon-reload"
                                 :onClick #(rf/dispatch [:patients/patients-reload]) } "Reload"]
                 [:> SearchBox {:style {:float "right" :margin "5px" :width "350px"}
                                :value (:text-like @filters)
                                :onChange #(rf/dispatch [:patients/update-data-filters [:text-like %]])}]
                 ]

                )
  ))

(defn column-render-text-like [value text-like]
  (if (and value text-like)
    (r/as-element [:span {:dangerouslySetInnerHTML
       {:__html (.replace value (js/RegExp. text-like "gi") 
                          #(str "<span style='color: red; background: yellow'>" % "</span>"))}}])
    value))

(defn datagrid []
  (let [data (rf/subscribe [:patients/data-filtered])
        filters (rf/subscribe [:patients/data-filters])]
;  (js/console.log "Data in table: " (count @data) @data (str @data))
  [:div
   [:> DataGrid {:data @data
                 :style {:height "100%"}
                 :selectionMode "single"
                 :toolbar datagrid-toolbar}
    [:> GridColumn {:width "30px" :align "center" :field "#" :title "#"
                    }]
    [:> GridColumn {:width "250px" :title "Patient name"
                     :render #(-> % .-row (aget "resource") (aget "patient_name")
                                 (column-render-text-like (:text-like @filters)))}]
    
    [:> GridColumn {:width "90px" :title "Birth date"
                    :render #(-> % .-row (aget "resource") (aget "birth_date")
                                 (column-render-text-like (:text-like @filters)))
                    }]

    [:> GridColumn {:width "70px" :title "Gender"
                    :render #(-> % .-row (aget "resource") (aget "gender")
                                 (column-render-text-like (:text-like @filters)))
                    }]
    [:> GridColumn {:width "50%" :title "Address"
                    :render #(-> % .-row (aget "resource") (aget "address")
                                 (column-render-text-like (:text-like @filters)))

                    }]
    [:> GridColumn {:width "50%"  :title "Policy number"
                    :render #(-> % .-row (aget "resource") (aget "policy_number")
                                 (column-render-text-like (:text-like @filters)))

                    }]
     ]]))

(defn dialog-create []
  (let [show-dialog (rf/subscribe [:patients/show-dialog])
        button-create-disabled (rf/subscribe [:patients/form-data-invalid])
        form-data (rf/subscribe [:patients/form-data])
        label-width "130px"
        ]
        [:> Dialog
         {:closed (not= @show-dialog :create)
          :onClose #(rf/dispatch [:patients/show-dialog nil])
          :title "Create patient"
          :modal true}
  [:div
   {:style {:padding "30px 20px"} :className "f-full"}
   [:> Form
    {:onValidate #(rf/dispatch [:patients/form-data-on-validate %])
     :rules {"patient_name" ["required" "length[5,100]"]
             "birth_date" ["required"]
             "gender" ["required"]
             "address" ["required"]
             "policy_number" {"required" true
                              "all-numbers" {"validator" #(re-find (js/RegExp "[\\d]{4}\\ [\\d]{4}\\ [\\d]{4}\\ [\\d]{4}") %)
                                             "message" "Need all digits"}}}
                                              
                                              
     :errorType "tooltip"
     :className "f-full"
     :model @form-data
     :onChange (fn [f v]
                 (when (not= f "birth_date")
                   (rf/dispatch [:patients/update-form-data [f v]])))}
;    [:div {:style {:margin-bottom "10px"}}
;     [:> Label {:htmlfor "inp_patient_name" :style {:width label-width}} "Patient name:"]
;     [:> TextBox {:name "patient_name"
;                  :inputId "inp_patient_name" :style {:width "400px"} :iconCls "icon-man"}]
                                        ;     ]
    [:> FormField {:name "patient_name"
                   :style {:margin-bottom "10px"} :labelAlign "right" :labelWidth label-width :label "Patient name: "}
     [:> TextBox {
                    :inputId "inp_patient_name" :style {:width "400px"} :iconCls "icon-man"}]    
     ]
    

;    [:div {:style {:margin-bottom "10px"}}
                                        ;     [:> Label {:htmlfor "inp_birth_date" :style {:width label-width}} "Birth date"]
    [:> FormField {:name "birth_date"
                   :style {:margin-bottom "10px"} :labelAlign "right" :labelWidth label-width :label "Birth date: "}
     [:> DateBox {:inputId "inp_birth_date" :format "yyyy-MM-dd"
                  :onChange #(rf/dispatch [:patients/update-form-data ["birth_date" (.getTime %)]])
                  :style {:width "200px"}}]
]
     
     #_[:div {:class "error"} "some error"]
    
 ;   [:div {:style {:margin-bottom "10px"}}
                                        ;     [:> Label {:htmlfor "inp_gender" :style {:width label-width}} "Gender:"]
  [:> FormField {:name "gender"
                   :style {:margin-bottom "10px"} :labelAlign "right" :labelWidth label-width :label "Gender: "}
     [:> ComboBox {:data [{:value "male" :text "Male"}{:value "female" :text "Female"}]
                   :inputId="inp_gender" :name "gender" :style {:width "200px"}}]
     #_[:div {:class "error"} "some error"]]

;    [:div {:style {:margin-bottom "10px"}}
                                        ;     [:> Label {:htmlfor "inp_address" :style {:width label-width}} "Address:"]
  [:> FormField {:name "address"
                   :style {:margin-bottom "10px"} :labelAlign "right" :labelWidth label-width :label "Address: "}    
     [:> TextBox {:inputId="inp_address" :style {:width "400px" :height "70px"} :multiline true}]
     #_[:div {:class "error"} "some error"]]

;    [:div {:style {:margin-bottom "10px"}}
                                        ;     [:> Label {:htmlfor "inp_policy_number" :style {:width label-width}} "Policy number:"]
      [:> FormField {:name "policy_number"
                   :style {:margin-bottom "10px"} :labelAlign "right" :labelWidth label-width :label "Policy number: "}
     [:> MaskedBox {:mask "9999 9999 9999 9999" :inputId="inp_policy_number"  :style {:width "155px"}}]
     #_[:div {:class "error"} "some error"]]
      
    ]]

   ;[:p "xx" @button-create-disabled]
         
   [:div {:className "dialog-button"}
     [:> LinkButton
      {:disabled @button-create-disabled
       :onClick #(rf/dispatch [:patients/send-event-create])
        :style {:width "80px"}} "Create"]
    ]
         
     ]))

(defn ui []
  [:div [datagrid] [dialog-create]])
