(ns frontend.patients.datagrid
  (:require
   ["rc-easyui" :refer [Layout LayoutPanel DataGrid GridColumn LinkButton Dialog Form TextBox DateBox SearchBox MaskedBox ComboBox FormField ButtonGroup]]
   [reagent.core :as r]
   [clojure.string :refer [trim replace blank?]]
   [frontend.modules :as rfm]
   [common.patients]
   [clojure.string :refer [trim replace blank?]]  
   [frontend.rf-isolate-ns :as rf-ns]
   [frontend.rf-nru-nwd :as rf-nru-nwd :refer [reg-sub]]   
;   [frontend.patients :refer [ui-patients-model]]
   [frontend.patients.models :as models]   
   [re-frame.core :as rf :refer [trim-v]]))

(def init-state {:filter-text-like nil
                 :selection nil                 
                 :loading false
                 :data []
                 :total 0
                 :pageSize 50
                 :pageNumber 1})

(reg-sub ::state #(-> %))

(defn state [] (rf/dispatch [::print-state]))

(defn- timestamp->human-date [ts]
  (let [date (new js/Date ts)
        y (.getFullYear date)
        m (inc (.getMonth date))
        d (.getDate date)]
    (str y "-" (when (< m 10) "0") m  
           "-" (when (< d 10) "0") d )))

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
      (-> app-state
        (assoc-in [:patients :selection] row)
        (assoc-in [:patients :form-data] row)))))


(defn column-render-text-like [value text-like]
  (if (and value text-like)
    (r/as-element [:span {:dangerouslySetInnerHTML
       {:__html (.replace value (js/RegExp. text-like "gi") 
                          #(str "<span style='color: red; background: yellow'>" % "</span>"))}}])
    value))

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

(rf/reg-event-fx ::patients-reload
                 (fn [cofx _]
                   (let [where (get-in cofx [:db :patients :remote-where])]
    (assoc cofx :dispatch [:comm/send-event [:patients/read where 0 100]]))))

(rf/reg-event-db ::update-filter-text-like
  (fn [state [_ value]] (assoc state :filter-text-like value)))

(defn toolbar-buttons [{:keys [selection filter-text-like]}]
  [{:caption "Add" :class :LinkButton :iconCls "icon-add" :style {:margin "5px"}
     :onClick #(rf/dispatch [:frontend.patients/show-dialog :create])}
    
    {:caption "Reload" :class :LinkButton :iconCls "icon-reload" :style {:margin "5px"}
     :onClick #(rf/dispatch [::read])}

    {:caption "Delete" :class :LinkButton :iconCls "icon-cancel" :style {:margin "5px"}
     :disabled (not selection)
     :onClick #(rf/dispatch [:frontend.patients/show-dialog :delete])}

    {:caption "Update" :class :LinkButton :iconCls "icon-edit" :style {:margin "5px"}
     :disabled (not selection)
     :onClick #(rf/dispatch [:frontend.patients/show-dialog :update])}

    {:class :SearchBox :style {:float "right" :margin "5px" :width "350px"}
     :value filter-text-like
     :onChange #(rf/dispatch [::update-filter-text-like %])}])
  
(defn datagrid-toolbar [state]
  (r/as-element (into [:div]
     (for [tb (toolbar-buttons state)]
        [:> (case (:class tb)
               :LinkButton LinkButton
               :SearchBox SearchBox) tb (:caption tb)]))))

(defn entry []
  (let [state @(rf/subscribe [::state])
        {:keys [data selection filter-text-like]} state]
    [:div
     [:> DataGrid {:data [] ;@data
                   :style {:height "100%"}
                   :selectionMode "single"
                   :toolbar (partial datagrid-toolbar state)
                   :selection selection
                   :idField "uuid"
                   :virtualScroll true
                   :lazy true
                   :onPageChange (fn [a b c d] (js/console.log "onPageChange" a b c d))
                   :onRowClick #(rf/dispatch [:patients/on-selection-change [%]])}
    
    [:> GridColumn {:width "30px"  :title "#" :align "center"  :render #(inc (.-rowIndex %))}]
    [:> GridColumn {:width "250px" :title "Patient name" :field "patient_name"}]
    [:> GridColumn {:width "120px" :title "Birth date" :align "center" :field "birth_date"}]
    [:> GridColumn {:width "70px" :title "Gender" :field "gender"}]
    [:> GridColumn {:width "50%" :title "Address" :field "address"}]
    [:> GridColumn {:width "50%"  :title "Policy number" :field "policy_number"}]]]))

(rf/reg-event-fx :patients/read
  (fn [cofx [_ data]]
    (-> cofx
        (assoc-in [:db :patients :data] data)
        (assoc :dispatch [:patients/clear-data-filtered]))))

(rf/reg-event-db ::print-state
  (fn [state] (js/console.log "state" (str state))))
