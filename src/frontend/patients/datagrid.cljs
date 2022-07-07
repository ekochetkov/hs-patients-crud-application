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
                 :total 50
                 :pageSize 50
                 :pageNumber 1})

(reg-sub ::state #(-> %))

(rf/reg-event-db ::on-row-click
  (fn [state [_ row-from-datagrid]]
    (let [uuid (aget row-from-datagrid "uuid")
          row (first (filter #(= (:uuid %) uuid) (:data state)))]
      (assoc state :selection row))))

(rf/reg-event-db ::start-loading
  #(assoc % :loading true))

(rf/reg-event-db ::update-filter-text-like
  (fn [state [_ value]] (assoc state :filter-text-like value)))

;; Receive data from back
(rf/reg-event-db ::read
  (fn [state [_ [_ data]]]
    (assoc state :data data
                 :loading false)))

(defn- on-row-click [row]
  (rf/dispatch [::on-row-click row]))

(defn on-page-change [a b c d]
  (js/console.log "onPageChange" a b c d))

(defn- ts->human-date [ts]
  (let [date (new js/Date ts)
        y (.getFullYear date)
        m (inc (.getMonth date))
        d (.getDate date)]
    (str y "-" (when (< m 10) "0") m  
           "-" (when (< d 10) "0") d )))

(defn- mapping-data-from-back [data]
  (->> data
    (map (fn [row]
           (assoc (:resource row)
                  "uuid" (:uuid row)
                  "birth_date" (ts->human-date (get-in row [:resource "birth_date"])))))))

(defn- filter-data [pattern data]
  (->> data
    (filter
     (fn [row]
       (let [row-without-uuid (dissoc row "uuid")
             visible-vals (vals row-without-uuid)]
         (some #(re-find pattern %) visible-vals))))))

(defn- highlite [value field pattern]
  (if (not= field "uuid")
   (r/as-element
     [:span {:dangerouslySetInnerHTML {:__html
         (.replace value pattern
          #(str "<span style='color: red; background: yellow'>"
                % "</span>"))}}]) value)) 

(defn- highlite-data [pattern data]
  (->> data
       (map (fn [row]
               (->> row (reduce-kv
                    (fn [m k v] (assoc m k (highlite v k pattern)))
                     {}))))))

(reg-sub ::data
  (fn [state]
    (-> (:data state)
         mapping-data-from-back
         ((fn [data]
            (if-let [ftl (:filter-text-like state)]
              (let [pattern (js/RegExp. ftl "gi")]
                (-> data
                    ((partial filter-data pattern))
                    ((partial highlite-data pattern))))
              data))))))

(defn- toolbar-buttons [{:keys [selection filter-text-like]}]
  [{:caption "Add" :class :LinkButton :iconCls "icon-add" :style {:margin "5px"}
     :onClick #(rf/dispatch [:frontend.patients.dialog-create/show-dialog])}
    
    {:caption "Reload" :class :LinkButton :iconCls "icon-reload" :style {:margin "5px"}
     :onClick #(rf/dispatch [:frontend.patients/datagrid-reload])}

    {:caption "Delete" :class :LinkButton :iconCls "icon-cancel" :style {:margin "5px"}
     :disabled (not selection)
     :onClick #(rf/dispatch [:frontend.patients.dialog-delete/show-dialog :delete])}

    {:caption "Update" :class :LinkButton :iconCls "icon-edit" :style {:margin "5px"}
     :disabled (not selection)
     :onClick #(rf/dispatch [:frontend.patients/show-dialog :update])}

    {:class :SearchBox :style {:float "right" :margin "5px" :width "350px"}
     :value filter-text-like
     :onChange #(rf/dispatch [::update-filter-text-like %])}])

(defn- datagrid-toolbar [state]
  (r/as-element (into [:div]
     (for [tb (toolbar-buttons state)]
        [:> (case (:class tb)
               :LinkButton LinkButton
               :SearchBox SearchBox) tb (:caption tb)]))))

(defn entry []
  (let [state @(rf/subscribe [::state])
        data @(rf/subscribe [::data])
        {:keys [selection]} state]
    [:div
     [:> DataGrid {:data data
                   :style {:height "100%"}
                   :selectionMode "single"
                   :toolbar (partial datagrid-toolbar state)
                   :selection selection
                   :idField "uuid"
                   :pageSize 50
                   :virtualScroll true
                   :lazy true
                   :onPageChange on-page-change
                   :onRowClick on-row-click}
    
    [:> GridColumn {:width "30px"  :title "#" :align "center"  :render #(inc (.-rowIndex %))}]
    [:> GridColumn {:width "250px" :title "Patient name" :field "patient_name"}]
    [:> GridColumn {:width "120px" :title "Birth date" :align "center" :field "birth_date"}]
    [:> GridColumn {:width "70px" :title "Gender" :field "gender"}]
    [:> GridColumn {:width "50%" :title "Address" :field "address"}]
    [:> GridColumn {:width "50%"  :title "Policy number" :field "policy_number"}]]]))
