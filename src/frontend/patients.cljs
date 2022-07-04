(ns frontend.patients
  (:require
   ["rc-easyui" :refer [Layout LayoutPanel DataGrid GridColumn LinkButton Dialog Form TextBox DateBox SearchBox MaskedBox ComboBox FormField ButtonGroup]]
   [reagent.core :as r]
   [re-frame.core :as rf]))

(def db {:patients {:data []
                    :selection nil
                    :data-filtered []
                    :data-filters {:text-like ""}                    
                    :show-dialog nil
                    :form-data-invalid true
                    :form-data {}
                    :remote-filters {}
                    :form-errors {}}})

(rf/reg-sub :patients/form-data-invalid
  #(get-in % [:patients :form-data-invalid]))

(rf/reg-sub :patients/selection
  #(get-in % [:patients :selection]))

(rf/reg-sub :patients/show-dialog
  #(get-in % [:patients :show-dialog]))

(rf/reg-sub :patients/data-filtered
  #(get-in % [:patients :data-filtered]))

(rf/reg-sub :patients/form-data
  #(get-in % [:patients :form-data]))

(rf/reg-sub :patients/data-filters
  #(get-in % [:patients :data-filters]))

(rf/reg-event-db :patients/show-dialog
  (fn [app-db [_ dialog]]
    (assoc-in app-db [:patients :show-dialog] dialog)))

(rf/reg-event-db :patients/update-form-data
  (fn [app-state [_ [field value]]]
    (assoc-in app-state [:patients :form-data :resource field] value)))

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

(defn timestamp->human-date [ts]
  (let [date (new js/Date ts)
        y (.getFullYear date)
        m (inc (.getMonth date))
        d (.getDate date)]
    (str y "-" (when (< m 10) "0") m  
           "-" (when (< d 10) "0") d )))

(rf/reg-event-db :patients/filter-data
  (fn [app-state _]
    (let [data (get-in app-state [:patients :data])]
      (if-let [text-like (get-in app-state [:patients :data-filters :text-like])]
        (assoc-in app-state [:patients :data-filtered]
           (filter (fn [row]
                   (let [filter-vals (-> row
                                         :resource
                                         (assoc "birth_date" (timestamp->human-date (get-in row [:resource "birth_date"])))
                                         vals)]
                   (some #(re-find (js/RegExp. text-like "i") (str %)) filter-vals))) data))
        (assoc-in app-state [:patients :data-filtered] data)))))

(rf/reg-event-db :patients/on-selection-change
  (fn [app-state [_ [row-from-datagrid]]]
    (let [uuid (aget row-from-datagrid "uuid")
          row (first (filter #(= (:uuid %) uuid) (get-in app-state [:patients :data])))]
      (js/console.log row-from-datagrid row)      
      (-> app-state
        (assoc-in [:patients :selection] row)
        (assoc-in [:patients :form-data] row)))))

(rf/reg-event-fx :patients/data
  (fn [cofx [_ data]]
    (-> cofx
        (assoc-in [:db :patients :data] data)
        (assoc :dispatch [:patients/clear-data-filtered]))))

(rf/reg-event-fx :patients/patients-reload
  (fn [cofx _]
    (assoc cofx :dispatch [:comm/send-event [:patients/data {} 0 100]])))

(rf/reg-event-fx :patients/send-event-delete
  (fn [cofx _]
    (let [uuid (aget (get-in cofx [:db :patients :selection]) "uuid")]
        (assoc cofx :dispatch [:comm/send-event [:patients/delete uuid]]))))

(rf/reg-event-fx :patients/delete
  (fn [cofx _]
    (-> cofx
      (assoc-in [:db :patients :show-dialog] nil)
      (assoc-in [:db :patients :selection] nil)
      (assoc :dispatch [:patients/patients-reload]))))

(rf/reg-event-fx :patients/send-event-update
  (fn [cofx _]
    (let [row (get-in cofx [:db :patients :form-data])]
        (assoc cofx :dispatch [:comm/send-event [:patients/update row]]))))

(rf/reg-event-fx :patients/update
  (fn [cofx _]
    (-> cofx 
      (assoc-in [:db :patients :show-dialog] nil)
      (assoc :dispatch [:patients/patients-reload]))))


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
  (let [filters (rf/subscribe [:patients/data-filters])
        selection (rf/subscribe [:patients/selection])]
  (r/as-element [:div
                 [:> LinkButton {:style {:margin "5px"}
                                 :iconCls "icon-add"
                                 :onClick #(rf/dispatch [:patients/show-dialog :create]) } "Add"]
                 [:> LinkButton {:style {:margin "5px"}
                                 :iconCls "icon-reload"
                                 :onClick #(rf/dispatch [:patients/patients-reload]) } "Reload"]
                 [:> LinkButton {:disabled (not @selection)
                                 :style {:margin "5px"}
                                 :iconCls "icon-cancel"
                                 :onClick #(rf/dispatch [:patients/show-dialog :delete]) } "Delete"]
                 [:> LinkButton {:disabled (not @selection)
                                 :style {:margin "5px"}
                                 :iconCls "icon-edit"
                                 :onClick #(rf/dispatch [:patients/show-dialog :update]) } "Update"]
                 [:> LinkButton {:style {:margin "5px"}
                                 :iconCls "icon-filter"} "Filter"]
                 [:> SearchBox {:style {:float "right" :margin "5px" :width "350px"}
                                :value (:text-like @filters)
                                :onChange #(rf/dispatch [:patients/update-data-filters [:text-like %]])}]])))

(defn column-render-text-like [value text-like]
  (if (and value text-like)
    (r/as-element [:span {:dangerouslySetInnerHTML
       {:__html (.replace value (js/RegExp. text-like "gi") 
                          #(str "<span style='color: red; background: yellow'>" % "</span>"))}}])
    value))

(defn datagrid []
  (let [data (rf/subscribe [:patients/data-filtered])
        filters (rf/subscribe [:patients/data-filters])
        selection (rf/subscribe [:patients/selection])]
;  (js/console.log "Data in table: " (count @data) @data (str @data))
  [:div
   [:> DataGrid {:data @data
                 :style {:height "100%"}
                 :selectionMode "single"
                 :toolbar datagrid-toolbar
                 :selection selection
                 :idField "uuid"
                 :onRowClick #(rf/dispatch [:patients/on-selection-change [%]])} 
    [:> GridColumn {:width "30px" :align "center" :title "#"
                    :render #(inc (.-rowIndex %))}]
    [:> GridColumn {:width "250px" :title "Patient name"
                    :render #(-> % .-row (aget "resource") (aget "patient_name")
                            (column-render-text-like (:text-like @filters)))}]
    [:> GridColumn {:width "90px" :title "Birth date"
                    :render #(-> % .-row (aget "resource") (aget "birth_date") timestamp->human-date
                                 (column-render-text-like (:text-like @filters)))}]
    [:> GridColumn {:width "70px" :title "Gender"
                    :render #(-> % .-row (aget "resource") (aget "gender")
                                 (column-render-text-like (:text-like @filters)))}]
    [:> GridColumn {:width "50%" :title "Address"
                    :render #(-> % .-row (aget "resource") (aget "address")
                                 (column-render-text-like (:text-like @filters)))}]
    [:> GridColumn {:width "50%"  :title "Policy number"
                    :render #(-> % .-row (aget "resource") (aget "policy_number")
                                 (column-render-text-like (:text-like @filters)))}]]]))

(defn dialog-delete []
  (let [show-dialog (rf/subscribe [:patients/show-dialog])
        selection (rf/subscribe [:patients/selection])]
    (when (and @selection (= @show-dialog :delete))
      [:> Dialog
        {:closed (not= @show-dialog :delete)
         :onClose #(rf/dispatch [:patients/show-dialog nil])
         :title "Delete patient"
         :modal true}
        [:div {:style {:padding "30px 20px"} :className "f-full"}
         [:p (str "Delete paptient: " (get-in @selection [:resource "patient_name"])
                                 " (" (timestamp->human-date (get-in @selection [:resource "birth_date"])) ")?")]]

     [:div {:className "dialog-button"}
      [:> LinkButton {:onClick #(rf/dispatch [:patients/send-event-delete])
                      :style {:float "left" :width "80px"}} "Yes"]        
      [:> LinkButton {:onClick #(rf/dispatch [:patients/show-dialog nil])
                      :style {:width "80px"}} "No"]]])))

(defn dialog-create []
  (let [show-dialog (rf/subscribe [:patients/show-dialog])
        button-create-disabled (rf/subscribe [:patients/form-data-invalid])
        form-data (rf/subscribe [:patients/form-data])
        label-width "130px"]
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
                                        ; :model @form-data
     :model {}
     :onChange (fn [f v]
                 (when (not= f "birth_date")
                   (rf/dispatch [:patients/update-form-data [f v]])))}
    [:> FormField {:name "patient_name"
                   :style {:margin-bottom "10px"} :labelAlign "right" :labelWidth label-width :label "Patient name: "}
     [:> TextBox {:inputId "inp_patient_name" :style {:width "400px"} :iconCls "icon-man"}]]
    
    [:> FormField {:name "birth_date"
                   :style {:margin-bottom "10px"} :labelAlign "right" :labelWidth label-width :label "Birth date: "}
     [:> DateBox {:inputId "inp_birth_date" :format "yyyy-MM-dd"
                  :onChange #(rf/dispatch [:patients/update-form-data ["birth_date" (.getTime %)]])
                  :style {:width "200px"}}]]
    
    [:> FormField {:name "gender"
                   :style {:margin-bottom "10px"} :labelAlign "right" :labelWidth label-width :label "Gender: "}
     [:> ComboBox {:data [{:value "male" :text "Male"}{:value "female" :text "Female"}]
                   :inputId="inp_gender" :name "gender" :style {:width "200px"}}]]

    [:> FormField {:name "address"
                   :style {:margin-bottom "10px"} :labelAlign "right" :labelWidth label-width :label "Address: "}    
     [:> TextBox {:inputId="inp_address" :style {:width "400px" :height "70px"} :multiline true}]] 
    
    [:> FormField {:name "policy_number"
                   :style {:margin-bottom "10px"} :labelAlign "right" :labelWidth label-width :label "Policy number: "}
     [:> MaskedBox {:mask "9999 9999 9999 9999" :inputId="inp_policy_number"  :style {:width "155px"}}]]]]
   
   [:div {:className "dialog-button"}
     [:> LinkButton {:disabled @button-create-disabled
                     :onClick #(rf/dispatch [:patients/send-event-create])
                     :style {:width "80px"}} "Create"]]]))

(defn dialog-update []
  (let [show-dialog (rf/subscribe [:patients/show-dialog])
        button-create-disabled (rf/subscribe [:patients/form-data-invalid])
        form-data (rf/subscribe [:patients/form-data])        
        label-width "130px"]
  (when (and @form-data (= @show-dialog :update))
        [:> Dialog
         {:closed (not= @show-dialog :update)
          :onClose #(rf/dispatch [:patients/show-dialog nil])
          :title "Update patient"
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
     :model (get @form-data :resource)
     :onChange (fn [f v]
                 (when (not= f "birth_date")
                   (rf/dispatch [:patients/update-form-data [f v]])))}
    [:> FormField {:focused true
                   :name "patient_name"
                   :style {:margin-bottom "10px"} :labelAlign "right" :labelWidth label-width :label "Patient name: "}
     [:> TextBox {:value (get-in @form-data [:resource "patient_name"])
                  :style {:width "400px"} :iconCls "icon-man"}]]
    
    [:> FormField {:name "birth_date"
                   :style {:margin-bottom "10px"} :labelAlign "right" :labelWidth label-width :label "Birth date: "}
     [:> DateBox {:value (js/Date. (get-in @form-data [:resource "birth_date"]))
                  :format "yyyy-MM-dd"
                  :onChange #(rf/dispatch [:patients/update-form-data ["birth_date" (.getTime %)]])
                  :style {:width "200px"}}]]
    
    [:> FormField {:name "gender"
                   :style {:margin-bottom "10px"} :labelAlign "right" :labelWidth label-width :label "Gender: "}
     [:> ComboBox {:value (get-in @form-data [:resource "gender"])
                   :data [{:value "male" :text "Male"}{:value "female" :text "Female"}]
                   :inputId="inp_gender" :name "gender" :style {:width "200px"}}]]

    [:> FormField {:name "address"
                   :style {:margin-bottom "10px"} :labelAlign "right" :labelWidth label-width :label "Address: "}    
     [:> TextBox {:value (get-in @form-data [:resource "address"])
                  :inputId="inp_address" :style {:width "400px" :height "70px"} :multiline true}]] 
    
    [:> FormField {:name "policy_number"
                   :style {:margin-bottom "10px"} :labelAlign "right" :labelWidth label-width :label "Policy number: "}
     [:> MaskedBox {:value (get-in @form-data [:resource "policy_number"])
                    :mask "9999 9999 9999 9999" :inputId="inp_policy_number"  :style {:width "155px"}}]]]]
   
   [:div {:className "dialog-button"}
     [:> LinkButton {:disabled @button-create-disabled
                     :onClick #(rf/dispatch [:patients/send-event-update])
                     :style {:width "80px"}} "Update"]]])))


;(defn ui []
;  [:div [datagrid] [dialog-create] [dialog-delete] [dialog-update]])

(rf/reg-event-db :patietns/update-remote-filters
   (fn [app-state [_ [field value]]]
     (js/console.log "update-remote-filters" field value)
     (assoc-in app-state [:patients :remote-filters field] value)))

(rf/reg-sub :patients/remote-filters
  #(get-in % [:patients :remote-filters]))

(defn filter-layout-panel-content []
  (let [input-style {:width "100%"}
        remote-filters (rf/subscribe [:patients/remote-filters])
        bd-mode (:birth-date @remote-filters)]
    [:div {:style {:padding "10px"}}

    [:p "Patient name contains:"]
       [:> TextBox {:style input-style}]

    [:p "Adress contains:"]
    [:> TextBox {:style input-style}]

    [:p "Policy number equal:"]
       [:> TextBox {:style input-style}]

     [:p "Gender:"]
    [:> ButtonGroup {:selectionMode "single"}
     [:> LinkButton {:selected true
                     :onClick #(rf/dispatch
                                [:patietns/update-remote-filters [:gender :any]])} "Any"]
     [:> LinkButton {:onClick #(rf/dispatch
                                [:patietns/update-remote-filters [:gender :male]])}  "Male"]
     [:> LinkButton {:onClick #(rf/dispatch
                                [:patietns/update-remote-filters [:gender :female]])} "Female"]]

    [:p "Birth date:"]
    [:> ButtonGroup {:selectionMode "single"}
     [:> LinkButton {:selected true
                     :onClick #(rf/dispatch
                                [:patietns/update-remote-filters [:birth-date :any]])} "Any"]
     [:> LinkButton {:onClick #(rf/dispatch
                                [:patietns/update-remote-filters [:birth-date :equal]])} "Equal"]
     [:> LinkButton {:onClick #(rf/dispatch
                                [:patietns/update-remote-filters [:birth-date :after]])} "After"]
     [:> LinkButton {:onClick #(rf/dispatch
                                [:patietns/update-remote-filters [:birth-date :before]])} "Before"]
     [:> LinkButton {:onClick #(rf/dispatch
                                [:patietns/update-remote-filters [:birth-date :between]])}"Between"]]

     (when (some #(= bd-mode %) '(:equal :after :before :between))
       [:p (when (= bd-mode :between) "Start: ") [:> DateBox {:style input-style}] ] )

     (when (= bd-mode :between)
       [:p "End: " [:> DateBox {:style input-style}] ])

     [:hr]

     [:> LinkButton "Reset"]
     [:> LinkButton {:style {:float "right"}}"Apply"]

    ]

    ))

(defn ui []
  [:> Layout {:style {"width" "100%" "height" "100%"}}

       [:> LayoutPanel {:region "west"
                        :title "Patients filters"
                        :collapsible true
                        :expander true
                        :style {:width "300px"}}

        [filter-layout-panel-content]
        ]

   [:> LayoutPanel {:region "center" :style {:height "100%"}}
    [datagrid] ]
   ])
