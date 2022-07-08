(ns frontend.patients.datagrid
  (:require
   ["rc-easyui" :refer [Layout LayoutPanel DataGrid GridColumn LinkButton Dialog Form TextBox DateBox SearchBox MaskedBox ComboBox FormField ButtonGroup]]
   [reagent.core :as r]
   [clojure.string :refer [trim replace blank? join]]
   [frontend.modules :as rfm]
   [common.patients]
   [clojure.string :refer [trim replace blank?]]  
   [frontend.rf-isolate-ns :as rf-ns]
   [frontend.rf-nru-nwd :as rf-nru-nwd :refer [reg-sub]]   
;   [frontend.patients :refer [ui-patients-model]]
   [frontend.patients.models :as models]
   [frontend.comm :as comm]
   [re-frame.core :as rf :refer [trim-v]]))

(def init-state {:filter-text-like nil
                 :selection nil                 
                 :loading false
                 :where []
                 :data []
                 :total 0
                 :page-size 50
                 :page-number 1})

(reg-sub ::state #(-> %))

(rf/reg-event-db ::on-row-click
  (fn [state [_ row-from-datagrid]]
    (let [uuid (aget row-from-datagrid "uuid")
          row (first (filter #(= (:uuid %) uuid) (:data state)))]
      (assoc state :selection row))))

(rf/reg-event-db ::update-filter-text-like
  (fn [state [_ value]] (assoc state :filter-text-like value)))

(rf/reg-event-fx ::update-where
  (fn [cofx [_ where]]
     (-> cofx
         (assoc-in [:db :where] where)
         (assoc-in [:db :data] [])
         (assoc-in [:db :page-number] 1)
         (assoc-in [:db :total] 0)
         (assoc :fx [[:dispatch [::datagrid-reload]]])
         )))

(rf/reg-event-fx ::datagrid-reload
  (fn [cofx]
    (let [{:keys [where page-number page-size]} (:db cofx)]
      (-> cofx
        (assoc-in [:db :loading] true)
        (assoc :fx [[:dispatch [::comm/send-event ::read [:patients/read where page-number page-size]]]])))))

(rf/reg-event-fx ::on-page-change
  (fn [cofx [_ page-number page-size]]
    (-> cofx
      (assoc-in [:db :page-number] page-number)
      (assoc-in [:db :page-size] page-size)
      (assoc :fx [[:dispatch [::datagrid-reload]]]))))

;; Receive data from back
(rf/reg-event-db ::read
  (fn [state [_ [_ [total page-number page-size data]]]]
    (assoc state :data data
                 :total total
                 :page-number page-number
                 :page-size page-size
                 :loading false)))

(defn- on-row-click [row]
  (rf/dispatch [::on-row-click row]))

(defn on-page-change [event]
  (js/console.log "onPageChange")
  (let [page-number (.-pageNumber event)
        page-size (.-pageSize event)]
  (rf/dispatch [::on-page-change page-number page-size])
  ))

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
                  "policy_number" (->> (partition-all 4 (get-in row [:resource "policy_number"]))
                                       (map #(join %))
                                       (join " "))
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
;    (js/console.log "::data" (count (:data state)))
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
     :onClick #(rf/dispatch [::datagrid-reload])}

    {:caption "Delete" :class :LinkButton :iconCls "icon-cancel" :style {:margin "5px"}
     :disabled (not selection)
     :onClick #(rf/dispatch [:frontend.patients.dialog-delete/show-dialog])}

    {:caption "Update" :class :LinkButton :iconCls "icon-edit" :style {:margin "5px"}
     :disabled (not selection)
     :onClick #(rf/dispatch [:frontend.patients.dialog-update/show-dialog selection])}
   
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
        {:keys [selection total page-size page-number]} state]
    [:div
     [:> DataGrid {:data data
                   :style {:height "100%"}
                   :selectionMode "single"
                   :toolbar (partial datagrid-toolbar state)
                   :selection selection
                   :idField "uuid"
                   :pageSize page-size
                   :total total
                   :pageNumber page-number
                   :virtualScroll true
                   :lazy true
                   :onPageChange on-page-change
                   :onRowClick on-row-click}
    
    [:> GridColumn {:width "40px"  :title "#" :align "center"  :render #(inc (.-rowIndex %))}]
    [:> GridColumn {:width "250px" :title "Patient name" :field "patient_name"}]
    [:> GridColumn {:width "120px" :title "Birth date" :align "center" :field "birth_date"}]
    [:> GridColumn {:width "70px"  :title "Gender" :field "gender"}]
    [:> GridColumn {:width "180px" :title "Policy number" :align "center" :field "policy_number"}]
    [:> GridColumn {:width "100%"  :title "Address" :field "address"}]]]))
