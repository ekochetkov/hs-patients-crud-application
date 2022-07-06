(ns frontend.patients.dialog-create
  (:require
   ["rc-easyui" :refer [Layout LayoutPanel DataGrid GridColumn LinkButton Dialog Form TextBox DateBox SearchBox MaskedBox ComboBox FormField ButtonGroup]]
   [reagent.core :as r]
   [clojure.string :refer [trim replace blank?]]
   [frontend.modules :as rfm]
   [common.patients]
   [clojure.string :refer [trim replace blank?]]  
   [frontend.rf-isolate-ns :as rf-ns]
;   [frontend.patients :refer [ui-patients-model]]
   [frontend.patients.models :as models]   
   [re-frame.core :as rf]))

(rf/reg-sub ::state #(get % *ns*))

(rf-ns/reg-event-fx ::init-state
  (fn [_] {:db {:form-data {}
                :is-valid-form-data false} :fx []}))
    
(rf-ns/reg-event-db ::on-change-form-data
  (fn [state [_ field-name value]]
    (assoc state [:form-data field-name] value)))

(rf-ns/reg-event-db ::on-validate-form-data
  (fn [module-state [_ errors]]
    (assoc module-state :is-valid-form-data (nil? errors))))

(defn- on-change-form-data [field-name value]
  (rf/dispatch [::on-change-form-data field-name value]))

(defn- on-validate-form-data [errors]
  (rf/dispatch [::on-validate-form-data errors]))

(defn- on-dialog-close []
  (rf/dispatch [:frontend.patients/show-dialog nil]))

(defn- on-create-button-click []
  (rf/dispatch [:frontend.patients/send-event-create]))

(defn- create-form-field [f-name f-data]
  (let[{:keys [label rc-input-class rc-input-attrs]} f-data]
  [:> FormField {:name f-name
                 :labelAlign "right"
                 :labelWidth "130px"
                 :label (str label ": ")}
   [:> (case rc-input-class
         :TextBox TextBox
         :DateBox DateBox
         :ComboBox ComboBox
         :MaskedBox MaskedBox)
       rc-input-attrs]]))

(defn- form []
  (let [form-data
        (:form-data @(rf/subscribe [::state]))]
    (into [:> Form
            {:errorType "tooltip"
             :className "f-full"
             :model {};form-data
             :rules common.patients/validation-rules
             :onChange on-change-form-data
             :onValidate on-validate-form-data}]
           (for [[f-name f-data] models/Patient]
                  (create-form-field f-name f-data)))))

(defn- footer []
  (let [button-create-disabled
        (:is-valid-form-data {}
         ;@(rfm/subscribe [::state]) 
         )]
    [:div {:className "dialog-button"}
      [:> LinkButton {:disabled (not button-create-disabled)
                      :onClick on-create-button-click
                      :style {:width "80px"}} "Create"]]))
    
(defn entry []
  [:> Dialog
   {:title "Create patient"
    :modal true
    :onClose on-dialog-close
    :style {:width "550px"}}
   [:div
    {:style {:padding "30px 20px"} :className "f-full"}
     [form]]
     [footer]])
