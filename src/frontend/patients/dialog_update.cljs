(ns frontend.patients.dialog-update
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

(rf/reg-event-fx :patients/update
  (fn [cofx _] 
    (-> cofx 
      (assoc-in [:db :patients :show-dialog] nil)
      (assoc :dispatch [:patients/patients-reload]))))

(rf/reg-event-fx :patients/send-event-update
  (fn [cofx _]
    (let [row (get-in cofx [:db :patients :form-data])]
        (assoc cofx :dispatch [:comm/send-event [:patients/update row]]))))


(def init-state {:form-data {}
                 :is-valid-form-data false})

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
