(ns frontend.patients.dialog-update
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
   [frontend.rf-nru-nwd :as rf-nru-nwd :refer [reg-sub]]
   [common.ui-anchors.patients.dialog-update :as anchors]   
   [frontend.comm :as comm]
   [re-frame.core :as rf]))

(def init-state {:dialog-closed true
                 :uuid nil
                 :form-data {}
                 :is-valid-form-data false
                 :backend-validation-rules nil})

(reg-sub ::state #(-> %))

(rf/reg-event-db ::show-dialog
   (fn [state [_ selection]]
     (assoc state :dialog-closed false
                  :uuid (:uuid selection)
                  :form-data (:resource selection)
                  :is-valid-form-data false
                  :backend-validation-rules nil)))

(rf/reg-event-db ::close-dialog
  #(assoc % :dialog-closed true
            :backend-validation-rules nil))

(rf/reg-event-fx ::send-event-update
  (fn [cofx _]
    (let [data (get-in cofx [:db :form-data])
          uuid (get-in cofx [:db :uuid])]
      (assoc {:db (:db cofx)} :fx [[:dispatch [::comm/send-event ::update [:patients/update uuid data]]]]))))

(rf/reg-event-fx ::update
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
         (assoc-in [:form-data field-name] value)
         (assoc-in [:backend-validation-rules] nil))))

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

(defn- on-update-button-click []
  (rf/dispatch [::send-event-update]))

(defn- create-form-field [locale model f-name f-data f-value]
  (let[{:keys [name rc-input-class rc-input-attrs]} f-data
       get-fn (get-in model [:converts name :get] #(-> %))
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
    (assoc rc-input-attrs :value (get-fn f-value)) ]]]))

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
            {:errorType "tooltip"
             :className "f-full"
             :model form-data
             :rules rules-translated
             :onChange (partial on-change-form-data patient-model)
             :onValidate on-validate-form-data}]
          (for [[f-name f-data] (:fields patient-model)]
              (create-form-field locale patient-model f-name f-data (get form-data (:name f-data)))))))

(defn- footer [locale state]
  (let [button-update-disabled (:is-valid-form-data state)]
    [:div {:className "dialog-button"}
      [:span {:id anchors/footer-save-button}
      [:> LinkButton {:disabled (not button-update-disabled)
                      :onClick on-update-button-click
                      :style {:width "80px"}} (:dialog-update.button-update locale)]]]))
    
(defn entry [locale patient-model]
  (let [state @(rf/subscribe [::state])
        closed (:dialog-closed state)]
     [:> Dialog
      {:title (:dialog-update.caption locale)
       :closed closed
       :modal true
       :onClose on-dialog-close
       :style {:width "550px"}}
      [:div
       {:id anchors/dialog-form
        :style {:padding "30px 20px"} :className "f-full"}
        [form locale state patient-model]]
      [footer locale state]]))

       
