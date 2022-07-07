(ns frontend.patients.filter-panel
  (:require
   ["rc-easyui" :refer [Layout LayoutPanel DataGrid GridColumn LinkButton Dialog Form TextBox DateBox SearchBox MaskedBox ComboBox FormField ButtonGroup]]
   [reagent.core :as r]
   [clojure.string :refer [trim replace blank?]]
   [frontend.modules :as rfm]
   [common.patients]
   [clojure.string :refer [trim replace blank?]]  
   [frontend.rf-isolate-ns :as rf-ns]
   [frontend.rf-nru-nwd :as rf-nru-nwd]   
;   [frontend.patients :refer [ui-patients-model]]
   [frontend.patients.models :as models]   
   [re-frame.core :as rf]))

(def init-state {})

(rf/reg-sub :patients/remote-where
  #(get-in % [:patients :remote-where]))
                    
(rf/reg-sub :patients/remote-filters
  #(get-in % [:patients :remote-filters]))

(rf/reg-event-db :patients/reset-remote-filters
  (fn [app-state _]
     (assoc-in app-state [:patients :remote-filters] {})))

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
