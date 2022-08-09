(ns frontend.patients.dialog-create
  (:require
   ["rc-easyui" :refer [LinkButton
                        Dialog
                        Form
                        TextBox
                        DateBox
                        MaskedBox
                        ComboBox
                        FormField]]
   [common.patients]
   [frontend.comm :as comm]   
   [frontend.rf-nru-nwd :as rf-nru-nwd :refer [reg-sub]]   
   [frontend.utils :refer [with-id]]
   [re-frame.core :as rf]))

(def init-state {:dialog-closed true
                 :form-data {} 
                 :is-valid-form-data false})

(reg-sub ::state #(-> %))

(rf/reg-event-db ::show-dialog
   #(assoc % :dialog-closed false
             :form-data {}
             :is-valid-form-data false))

(rf/reg-event-db ::close-dialog
  #(assoc % :dialog-closed true))

(rf/reg-event-fx ::send-event-create   
  (fn [cofx _]
    (let [data (get-in cofx [:db :form-data])]
      (assoc cofx :fx [[:dispatch [::comm/send-event ::create [:patients/create data]]]]))))

(rf/reg-event-fx ::create 
  (fn [cofx]
    (-> cofx
      (assoc-in [:db :dialog-closed] true)
      (assoc-in [:db :form-data] {})
      (assoc :fx [[:dispatch [:frontend.patients.datagrid/datagrid-reload]]]))))
    
(rf/reg-event-db ::on-change-form-data
  (fn [state [_ field-name value]]
    (assoc-in state [:form-data field-name] value)))

(rf/reg-event-db ::on-validate-form-data
                 (fn [module-state [_ errors]]
                   (js/console.log "valie" (str module-state))
    (assoc module-state :is-valid-form-data (nil? errors))))

(defn- on-change-form-data [model field-name value]
  (let [set-fn (get-in model [:converts field-name :set] #(-> %))]
     (rf/dispatch [::on-change-form-data field-name (set-fn value)])))
   
(defn- on-validate-form-data [errors]
  (rf/dispatch [::on-validate-form-data errors]))

(defn- on-dialog-close []
  (rf/dispatch [::close-dialog]))

(defn- on-create-button-click []
  (rf/dispatch [::send-event-create]))

(defn- create-form-field [locale f-name f-data]
  (let[{:keys [name rc-input-class rc-input-attrs]} f-data]
    (with-id name
      [:> FormField {:name name
                 :labelAlign "right"
                 :labelWidth "160px"
                 :label (str (f-name locale) ": ")}
       [:> (case rc-input-class
             :TextBox TextBox
             :DateBox DateBox
             :ComboBox ComboBox
             :MaskedBox MaskedBox)
            (rc-input-attrs locale)]])))

(defn- form [locale state patient-model]
  (let [form-data (:form-data state)]
    (into [:> Form
            {:errorType "tooltip"
             :className "f-full"
             :model form-data
             :patientModel patient-model
             :rules common.patients/validation-rules
             :onChange (partial on-change-form-data patient-model)
             :onValidate on-validate-form-data
             }]
           (for [[f-name f-data] (:fields patient-model)]
                  (create-form-field locale f-name f-data)))))

(defn- footer [locale state]
  (let [button-create-disabled (:is-valid-form-data state)]
    [:div {:className "dialog-button"}
     (with-id "patients-dialog_create-button-create"
       [:> LinkButton {:disabled (not button-create-disabled)
                      :onClick on-create-button-click
                      :style {:width "80px"}} (:dialog-create.button-create locale)])]))
    
(defn entry [locale patient-model]
  (let [state @(rf/subscribe [::state])
        closed (:dialog-closed state)]
    [:> Dialog
     {:title (:dialog-create.caption locale)
      :closed closed
      :modal true
      :onClose on-dialog-close
      :style {:width "550px"}}
     [:div
      {:style {:padding "30px 20px"} :className "f-full"}
       [form locale state patient-model]]
       [footer locale state]]))
