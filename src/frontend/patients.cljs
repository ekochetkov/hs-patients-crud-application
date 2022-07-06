(ns frontend.patients
  (:require
   ["rc-easyui" :refer [Layout LayoutPanel DataGrid GridColumn LinkButton Dialog Form TextBox DateBox SearchBox MaskedBox ComboBox FormField ButtonGroup]]
   [reagent.core :as r]
   [clojure.string :refer [trim replace blank?]]
   [frontend.modules :as rfm]
   [common.patients]
   [frontend.patients.dialog-create :as dialog-create] 
   [frontend.rf-isolate-ns :as rf-ns]
   [re-frame.core :as rf]))

;(reg-event-fx              ;; -fx registration, not -db registration
;  :my-event
;  (fn [cofx [_ a]]        ;; 1st argument is coeffects, instead of db
;    {:db       (assoc (:db cofx) :flag  a)
;     :fx       [[:dispatch [:do-something-else 3]]]})) ;; return effects

;(let [kw ::some-key]
                                        ;   (js/console.log "xxx" (str kw) (name kw) (namespace kw)))

;(rf-ns/reg-event-db ::some-event-in-patients-ns
;                    (fn [ns-state [_ a b]]
;                      (js/console.log "xxx" (str ns-state) a b)
                                        ;

                                        ;(assoc ns-state :a a :b b :sum (+ a b))))

;(rf/reg-event-fx ::init-db
;  (fn [cofx _]               

(rf-ns/reg-event-fx ::init-state
   (fn [cofx _]
     {:db {}
      :fx [[:dispatch [::dialog-create/init-state]]]}))

;                      {:form-data {}
;           :is-valid-form-data false}))

(def db {:data []
         :selection nil
         :data-filtered []
         :data-filters {:text-like ""}
         :show-dialog nil
         :form-data-invalid true
         :form-data {}
         :remote-filters {}
         :remote-where []

;         "frontend.patients.dialog-create" dialog-create/init-state

         :component-dialog-create {:form-data {}
                                   :is-valid-form-data false}

         :component-dialog-update {:form-data {}
                                   :is-valid-form-data false}
         
         :datagrid-state {:selection nil
                          :loading false
                          :data []
                          :total 0
                          :pageSize 50
                          :pageNumber 1}
         :form-errors {}})

(defn- timestamp->human-date [ts]
  (let [date (new js/Date ts)
        y (.getFullYear date)
        m (inc (.getMonth date))
        d (.getDate date)]
    (str y "-" (when (< m 10) "0") m  
           "-" (when (< d 10) "0") d )))

(def locales {:en {:human-date-format "yyyy-MM-dd"}
              :ru {:human-date-format "dd.MM.yyyy"}})

(def locale (:en locales))


(rfm/reg-event-db :show-dialog
  (fn [module-state [_ dialog-name]]
    (assoc module-state :show-dialog dialog-name)))

(rfm/reg-sub :show-dialog #(:show-dialog %))

;(defn- set-field-ui-patients-model [field-name value]
;  (if-let [set-fn (get-in ui-patients-model [field-name :set-fn])]
;    (set-fn value) value))

;; Dialog create patient


(rf/reg-sub :app-state #(str %))





(rf/reg-sub :patients #(:patients %))

(rf/reg-sub :patients/form-data-invalid
  #(get-in % [:patients :form-data-invalid]))

(rf/reg-sub :patients/selection
  #(get-in % [:patients :selection]))

(rf/reg-sub :patients/data-filtered
  #(get-in % [:patients :data-filtered]))

(rf/reg-sub :patients/form-data
  #(get-in % [:patients :form-data]))

(rf/reg-sub :patients/data-filters
  #(get-in % [:patients :data-filters]))

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
;      (js/console.log row-from-datagrid row)
      (-> app-state
        (assoc-in [:patients :selection] row)
        (assoc-in [:patients :form-data] row)))))

(rf/reg-event-fx :patients/read
  (fn [cofx [_ data]]
    (-> cofx
        (assoc-in [:db :patients :data] data)
        (assoc :dispatch [:patients/clear-data-filtered]))))

(rf/reg-event-fx :patients/patients-reload
                 (fn [cofx _]
                   (let [where (get-in cofx [:db :patients :remote-where])]
    (assoc cofx :dispatch [:comm/send-event [:patients/read where 0 100]]))))

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
                                 :onClick

                                 #(rf/dispatch [::some-event-in-patients-ns 2 2])}

                                 
                                        ;#(rfm/dispatch [:patients/show-dialog :create]) } "Add"
                                 ]
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
                 :virtualScroll true
                 :lazy true
                 :onPageChange (fn [a b c d] (js/console.log "onPageChange" a b c d))
                 :onRowClick #(rf/dispatch [:patients/on-selection-change [%]])} 
    [:> GridColumn {:width "30px" :align "center" :title "#"
                    :render #(inc (.-rowIndex %))}]
    [:> GridColumn {:width "250px" :title "Patient name"
                    :render #(-> % .-row (aget "resource") (aget "patient_name")
                            (column-render-text-like (:text-like @filters)))}]
    [:> GridColumn {:width "120px" :align "center" :title "Birth date"
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

(rf/reg-event-db :patients/apply-remote-filters
                 (fn [app-state _]
                   (let [remote-filters (get-in app-state [:patients :remote-filters])
                         patient-name (:patient-name remote-filters)
                         address (:address remote-filters)
                         gender (:gender remote-filters)
                         policy-number (:policy-number remote-filters)
                         birth-date (:birth-date remote-filters)
                         birth-date-start (:birth-date-start remote-filters)
                         birth-date-end (:birth-date-end remote-filters)]
;                     (js/console.log "rf" (str remote-filters))
                     (assoc-in app-state [:patients :remote-where] (cond-> []
                       patient-name (conj [:like "patient_name" (str "%" patient-name "%")])
                       address (conj [:like :address (str "%" address "%")])
                       policy-number (conj [:= "policy_number" policy-number ])
                       gender (conj [:= :gender (gender {:male "male" :female "female"})])
                                    birth-date (conj (case birth-date
                                                       :equal   [:=  :birth-date birth-date-start]
                                                       :after   [:=> :birth-date birth-date-start]
                                                       :before  [:=< :birth-date birth-date-start]
                                                       :between [:between :birth-date
                                                                          birth-date-start
                                                                 birth-date-end]))))
                     )

                   )
                 
                 )

(rf/reg-event-db :patients/reset-remote-filters
  (fn [app-state _]
     (assoc-in app-state [:patients :remote-filters] {})))
                   

(rf/reg-event-db :patietns/update-remote-filters
  (fn [app-state [_ [field value]]]
;     (js/console.log "update-remote-filters" field value (blank? value) (type value))
;     (js/console.log "dissoc" (str (dissoc (get-in app-state [:patients :remote-filters]) field)))
     
    (cond-> app-state
      
       (blank? value)
          (assoc-in [:patients :remote-filters]
            (dissoc (get-in app-state [:patients :remote-filters]) field))

       (and (blank? value) (= field :birth-date))
          (assoc-in [:patients :remote-filters]
            (dissoc (get-in app-state [:patients :remote-filters]) field
               :birth-date-start :birth-date-end))
         
       (not (blank? value))
         (assoc-in [:patients :remote-filters field] value)

)))
         
(rf/reg-sub :patients/remote-where
  #(get-in % [:patients :remote-where]))
                    
(rf/reg-sub :patients/remote-filters
  #(get-in % [:patients :remote-filters]))

(defn filter-layout-panel-content []
  (let [input-style {:width "100%"}
        remote-filters (rf/subscribe [:patients/remote-filters])
        bd-mode (:birth-date @remote-filters)]
;(js/console.log "where" (str @(rf/subscribe [:patients/remote-where])))
    
    [:div {:style {:padding "10px"}}

    [:p "Patient name contains:"]
     [:> TextBox {:style input-style
                  :value (:patient-name @remote-filters)
                  :onChange #(rf/dispatch
                                [:patietns/update-remote-filters [:patient-name %]])
                  }]

    [:p "Adress contains:"]
     [:> TextBox {:style input-style
                  :value (:address @remote-filters)
                  :onChange #(rf/dispatch
                                [:patietns/update-remote-filters [:address %]])

                  }]

    [:p "Policy number equal:"]
     [:> TextBox {:style input-style
                  :value (:policy-number @remote-filters)
                  :onChange #(rf/dispatch
                               [:patietns/update-remote-filters [:policy-number %]])
                  }]


     [:p {:style {:font-weight (when (not (blank? (:gender @remote-filters) )) "bold")}}

      "Gender:"]
    [:> ButtonGroup {:selectionMode "single"}
     [:> LinkButton {:selected (nil? (:gender @remote-filters))
                     :onClick #(rf/dispatch
                                [:patietns/update-remote-filters [:gender nil]])} "Any"]
     [:> LinkButton {:selected (= :male (:gender @remote-filters))
                     :onClick #(rf/dispatch
                                [:patietns/update-remote-filters [:gender :male]])}  "Male"]
     [:> LinkButton {:selected (= :female (:gender @remote-filters))
                     :onClick #(rf/dispatch
                                [:patietns/update-remote-filters [:gender :female]])} "Female"]]

     [:p {:style {:font-weight (when (not (blank? bd-mode)) "bold")}}
                  "Birth date:"]
    [:> ButtonGroup {:selectionMode "single"}
     [:> LinkButton {:selected (nil? bd-mode)
                     :onClick #(rf/dispatch
                                [:patietns/update-remote-filters [:birth-date nil]])} "Any"]
     [:> LinkButton {:selected (= bd-mode :equal)
                     :onClick #(rf/dispatch
                                [:patietns/update-remote-filters [:birth-date :equal]])} "Equal"]
     [:> LinkButton {:onClick #(rf/dispatch
                                [:patietns/update-remote-filters [:birth-date :after]])} "After"]
     [:> LinkButton {:onClick #(rf/dispatch
                                [:patietns/update-remote-filters [:birth-date :before]])} "Before"]
     [:> LinkButton {:onClick #(rf/dispatch
                                [:patietns/update-remote-filters [:birth-date :between]])}"Between"]]

     (when (some #(= bd-mode %) '(:equal :after :before :between))
       [:p (when (= bd-mode :between) "Start: ")
        [:> DateBox {:style input-style
                     :value (:birth-date-start @remote-filters)
                     :onChange #(rf/dispatch
                               [:patietns/update-remote-filters [:birth-date-start %]])

                     }] ] )

     (when (= bd-mode :between)
       [:p "End: " [:> DateBox {:style input-style

                     :value (:birth-date-end @remote-filters)
                     :onChange #(rf/dispatch
                               [:patietns/update-remote-filters [:birth-date-end %]])
                                

                                }] ])

     [:hr]

     [:> LinkButton {:onClick #(rf/dispatch [:patients/reset-remote-filters])} "Reset"]
     [:> LinkButton {:style {:float "right"}
                     :onClick #(rf/dispatch [:patients/apply-remote-filters])} "Apply"]

     [:p (str @(rf/subscribe [:patients/remote-where])) ]
          
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

   [:> LayoutPanel {:region "south"} (str @(rf/subscribe [:patients/remote-filters]))]

   (let [patients-state @(rf/subscribe [:app-state])]
;         update-in patient]
      [:> LayoutPanel {:region "east" :style {:width "300px"}} patients-state]
   )

   [:> LayoutPanel {:region "center" :style {:height "100%"}}
    [datagrid] [dialog-create/entry] ]
   ])
