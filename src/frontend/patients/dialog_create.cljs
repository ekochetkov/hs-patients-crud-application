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
   [common.ui-anchors.patients.dialog-create :as anchors]
   [frontend.rf-nru-nwd :as rf-nru-nwd :refer [reg-sub]]   
   [frontend.utils :refer [with-id]]
   [re-frame.core :as rf]))

(def init-state {:dialog-closed true
                 :form-data {} 
                 :is-valid-form-data false
                 :backend-validation-rules nil})

(reg-sub ::state #(-> %))

(rf/reg-event-db ::show-dialog
   #(assoc % :dialog-closed false
             :form-data {}
             :is-valid-form-data false
             :backend-validation-rules nil))

(rf/reg-event-db ::close-dialog
   #(assoc % :dialog-closed true
             :backend-validation-rules nil))

(rf/reg-event-fx ::send-event-create   
  (fn [cofx _]
    (let [data (get-in cofx [:db :form-data])]
      (assoc {:db (:db cofx)} :fx [[:dispatch [::comm/send-event ::create [:patients/create data]]]]))))

(rf/reg-event-fx ::create 
  (fn [cofx [_ [_ {:keys [success rules]}]]]
    (let [init-cofx {:db (:db cofx)}]
      (if success
        (-> init-cofx
            (assoc-in [:db :dialog-closed] true)
            (assoc-in [:db :form-data] {})
            (assoc :fx [[:dispatch [:frontend.patients.datagrid/datagrid-reload]]]))
        (-> init-cofx
            (assoc-in [:db :backend-validation-rules] rules))))))
    
(rf/reg-event-db ::on-change-form-data
  (fn [state [_ field-name value]]
    (-> state
        (assoc-in [:backend-validation-rules] nil)
        (assoc-in [:form-data field-name] value))))

(rf/reg-event-db ::on-validate-form-data
  (fn [module-state [_ errors]]
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
  (let[{:keys [name rc-input-class rc-input-attrs]} f-data
       rc-input-attrs (rc-input-attrs locale)]
    [:span {:id name :type "field" :fieldtype
            (case rc-input-class
              :TextBox (if (:multiline rc-input-attrs)
                          "TextArea"
                          "TextBox")
              rc-input-class)}
      [:> FormField {:name name
                 :labelAlign "right"
                 :labelWidth "160px"
                 :label (str (f-name locale) ": ")}
       [:> (case rc-input-class
             :TextBox TextBox
             :DateBox DateBox
             :ComboBox ComboBox
             :MaskedBox MaskedBox)
            rc-input-attrs]]]))

(defn- form [locale state patient-model]
  (let [form-data (:form-data state)
        rules     (if-let [bvr (:backend-validation-rules state)]
                    (->> bvr
                         (reduce (fn [acc [f-name f-message]]
                                   (assoc acc f-name {"required" true
                                                      "rule"     {"validator" #(-> false)
                                                                  "message"   f-message}})) {}))
                    common.patients/validation-rules)
        rules-translated (->> rules
                              (reduce (fn [acc [rule-name rule-body]]
                                (assoc acc
                                       rule-name
                                       (if-let [message-code (get-in rule-body ["rule" "message"] )]
                                         (assoc-in rule-body
                                                   ["rule" "message"]
                                                   (get-in locale [:validate-rule-message message-code]))
                                         rule-body))) {} ))]
    (into [:> Form
           {:errorType    "tooltip"
            :className    "f-full"
            :model        form-data
            :patientModel patient-model
            :rules        rules-translated
            :onChange     (partial on-change-form-data patient-model)
            :onValidate   on-validate-form-data}]
          (for [[f-name f-data] (:fields patient-model)]
                  (create-form-field locale f-name f-data)))))

(defn- footer [locale state]
  (let [button-create-disabled (:is-valid-form-data state)]
    [:div {:className "dialog-button"}
     (with-id anchors/footer-add-button
       [:> LinkButton {:disabled (not button-create-disabled)
                      :onClick on-create-button-click
                      :style {:width "80px"}} (:dialog-create.button-create locale)])]))
    
(defn entry [locale patient-model]
  (let [state @(rf/subscribe [::state])
        closed (:dialog-closed state)]
     [:> Dialog
      {:title (:dialog-create.caption locale)
       :closed closed
       :id "ddeffd"
       :modal true
       :onClose on-dialog-close
       :style {:width "550px"}}
      [:div
       {:id anchors/dialog-form
        :type :form
        :style {:padding "30px 20px"} :className "f-full"}
       [form locale state patient-model]]
        [footer locale state]]))
