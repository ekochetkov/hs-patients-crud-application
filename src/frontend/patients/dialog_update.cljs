(ns frontend.patients.dialog-update
  (:require
   ["rc-easyui" :refer [Layout LayoutPanel DataGrid GridColumn LinkButton Dialog Form TextBox DateBox SearchBox MaskedBox ComboBox FormField ButtonGroup]]
   [reagent.core :as r]
   [clojure.string :refer [trim replace blank?]]
   [frontend.modules :as rfm]
   [common.patients]
   [clojure.string :refer [trim replace blank?]]  
   [frontend.rf-isolate-ns :as rf-ns]
   [frontend.rf-nru-nwd :as rf-nru-nwd :refer [reg-sub]]
   [frontend.comm :as comm]
   [frontend.patients.models :as models]   
   [re-frame.core :as rf]))

(def init-state {:dialog-closed true
                 :uuid nil
                 :form-data {}
                 :is-valid-form-data false})

(reg-sub ::state #(-> %))

(rf/reg-event-db ::show-dialog
   (fn [state [_ selection]]
     (assoc state :dialog-closed false
                  :uuid (:uuid selection)
                  :form-data (:resource selection)
                  :is-valid-form-data false)))

(rf/reg-event-db ::close-dialog
  #(assoc % :dialog-closed true))

(rf/reg-event-fx ::send-event-update
  (fn [cofx [_ selection]]
    (let [data (get-in cofx [:db :form-data])
          uuid (get-in cofx [:db :uuid])]
      (assoc cofx :fx [[:dispatch [::comm/send-event ::update [:patients/update uuid data]]]]))))

(rf/reg-event-fx ::update
  (fn [cofx]
    (-> cofx
      (assoc-in [:db :dialog-closed] true)
      (assoc-in [:db :form-data] {})
      (assoc :fx [[:dispatch [:frontend.patients/datagrid-reload]]]))))
    
(rf/reg-event-db ::on-change-form-data
  (fn [state [_ field-name value]]
    (assoc-in state [:form-data field-name] value)))

(rf/reg-event-db ::on-validate-form-data
  (fn [module-state [_ errors]]
    (assoc module-state :is-valid-form-data (nil? errors))))

(defn- on-change-form-data [field-name value]
  (this-as this
    (let [set-fn (-> this
                   (aget "patientModel")
                   (aget field-name)
                   (aget "setFn"))
          new-value (if set-fn
                      (set-fn value)
                      value)]
    (rf/dispatch [::on-change-form-data field-name new-value]))))

(defn- on-validate-form-data [errors]
  (rf/dispatch [::on-validate-form-data errors]))

(defn- on-dialog-close []
  (rf/dispatch [::close-dialog]))

(defn- on-update-button-click []
  (rf/dispatch [::send-event-update]))

(defn- create-form-field [f-name f-data f-value]
  (let[{:keys [label rc-input-class rc-input-attrs get-fn]} f-data]
   [:> FormField {:name f-name
                  :labelAlign "right"
                  :labelWidth "130px"
                  :label (str label ": ")}
    [:> (case rc-input-class
          :TextBox TextBox
          :DateBox DateBox
          :ComboBox ComboBox
          :MaskedBox MaskedBox)
     (assoc rc-input-attrs :value (if get-fn
                                   (get-fn f-value)
                                   f-value))]]))

(defn- form [state patient-model]
  (let [form-data (:form-data state)]
    (into [:> Form
            {:errorType "tooltip"
             :className "f-full"
             :model form-data
             :patientModel patient-model
             :rules common.patients/validation-rules
             :onChange on-change-form-data
             :onValidate on-validate-form-data}]
          (for [[f-name f-data] patient-model]
                 (create-form-field f-name f-data (get form-data f-name))))))

(defn- footer [state]
  (let [button-update-disabled (:is-valid-form-data state)]
    [:div {:className "dialog-button"}
      [:> LinkButton {:disabled (not button-update-disabled)
                      :onClick on-update-button-click
                      :style {:width "80px"}} "Update"]]))
    
(defn entry [patient-model]
  (let [state @(rf/subscribe [::state])
        closed (:dialog-closed state)]
     [:> Dialog
      {:title "Update patient"
       :closed closed
       :modal true
       :onClose on-dialog-close
       :style {:width "550px"}}
      [:div
       {:style {:padding "30px 20px"} :className "f-full"}
        [form state patient-model]]
        [footer state]]))
