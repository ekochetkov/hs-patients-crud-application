(ns frontend.patients.dialog-delete
  (:require
   ["rc-easyui" :refer [LinkButton Dialog]]
   [common.patients]
   [goog.string :as gstring]
   [goog.string.format]
   [frontend.comm :as comm]   
   [frontend.utils :refer [ts->human-date]]   
   [frontend.rf-nru-nwd :as rf-nru-nwd :refer [reg-sub]]   
   [re-frame.core :as rf]))

(def init-state {:dialog-closed true})

(reg-sub ::state #(-> %))

(rf/reg-event-fx ::send-event-delete
  (fn [cofx [_ selection]]
    (let [uuid (:uuid selection)]
        (assoc cofx :fx [[:dispatch [::comm/send-event ::delete [:patients/delete uuid]]]]))))

(rf/reg-event-fx ::delete
  (fn [cofx]
    (-> cofx
      (assoc-in [:db :dialog-closed] true)
      (assoc :fx [[:dispatch [:frontend.patients.datagrid/on-row-click {}]]
                  [:dispatch [:frontend.patients.datagrid/datagrid-reload]]]))))

(rf/reg-event-db ::show-dialog
   #(assoc % :dialog-closed false
             :form-data {}
             :is-valid-form-data false))

(rf/reg-event-db ::close-dialog
  #(assoc % :dialog-closed true))

(defn- on-dialog-close []
  (rf/dispatch [::close-dialog]))

(defn- on-click-yes [selection]
  (rf/dispatch [::send-event-delete selection]))

(defn- on-click-no []
  (rf/dispatch [::close-dialog]))

(defn entry [selection {:keys [dialog-delete human-date-format]}]
  (let [state @(rf/subscribe [::state])
        closed (:dialog-closed state)
        
        ]
      [:> Dialog
        {:closed closed
         :onClose on-dialog-close
         :title (:caption dialog-delete)
         :modal true}
        [:div {:style {:padding "30px 20px"} :className "f-full"}
         [:p (gstring/format "%s: %s (%s)?"
               (:text dialog-delete)      
               (get-in selection [:resource "patient_name"])
               (ts->human-date (get-in selection [:resource "birth_date"]) human-date-format))]]
     [:div {:className "dialog-button"}
      [:> LinkButton {:onClick (partial on-click-yes selection)
                      :style {:float "left" :width "80px"}} (:yes dialog-delete)]        
      [:> LinkButton {:onClick on-click-no
                      :style {:width "80px"}} (:no dialog-delete)]]]))
