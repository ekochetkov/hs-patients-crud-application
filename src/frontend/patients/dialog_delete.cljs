(ns frontend.patients.dialog-delete
  (:require
   ["rc-easyui" :refer [Layout LayoutPanel DataGrid GridColumn LinkButton Dialog Form TextBox DateBox SearchBox MaskedBox ComboBox FormField ButtonGroup]]
   [reagent.core :as r]
   [clojure.string :refer [trim replace blank?]]
   [frontend.modules :as rfm]
   [common.patients]
   [frontend.comm :as comm]   
   [clojure.string :refer [trim replace blank?]]  
   [frontend.rf-isolate-ns :as rf-ns]
   [frontend.rf-nru-nwd :as rf-nru-nwd :refer [reg-sub]]   
   [frontend.patients.models :as models]   
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
      (assoc :fx [[:dispatch [:frontend.patients.datagrid/datagrid-reload]]]))))

(rf/reg-event-db ::show-dialog
   #(assoc % :dialog-closed false
             :form-data {}
             :is-valid-form-data false))

(rf/reg-event-db ::close-dialog
  #(assoc % :dialog-closed true))

(defn- ts->human-date [ts]
  (let [date (new js/Date ts)
        y (.getFullYear date)
        m (inc (.getMonth date))
        d (.getDate date)]
    (str y "-" (when (< m 10) "0") m  
           "-" (when (< d 10) "0") d )))

(defn- on-dialog-close []
  (rf/dispatch [::close-dialog]))

(defn- on-click-yes [selection]
  (rf/dispatch [::send-event-delete selection]))

(defn- on-click-no []
  (rf/dispatch [::close-dialog]))

(defn entry [selection]
  (let [state @(rf/subscribe [::state])
        closed (:dialog-closed state)]
      [:> Dialog
        {:closed closed
         :onClose on-dialog-close
         :title "Delete patient"
         :modal true}
        [:div {:style {:padding "30px 20px"} :className "f-full"}
         [:p (str "Delete paptient: " (get-in selection [:resource "patient_name"])
                                 " (" (ts->human-date (get-in selection [:resource "birth_date"])) ")?")]]

     [:div {:className "dialog-button"}
      [:> LinkButton {:onClick (partial on-click-yes selection)
                      :style {:float "left" :width "80px"}} "Yes"]        
      [:> LinkButton {:onClick on-click-no
                      :style {:width "80px"}} "No"]]]))
