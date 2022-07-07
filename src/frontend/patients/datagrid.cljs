(ns frontend.patients.datagrid
  (:require
   ["rc-easyui" :refer [Layout LayoutPanel DataGrid GridColumn LinkButton Dialog Form TextBox DateBox SearchBox MaskedBox ComboBox FormField ButtonGroup]]
   [reagent.core :as r]
   [clojure.string :refer [trim replace blank?]]
   [frontend.modules :as rfm]
   [common.patients]
   [clojure.string :refer [trim replace blank?]]  
   [frontend.rf-isolate-ns :as rf-ns]
   [frontend.rf-nru-nwd :as rf-nru-nwd]   
;   [frontend.patients :refer [ui-patients-model]]
   [frontend.patients.models :as models]   
   [re-frame.core :as rf]))

(def init-state {:selection nil
                 :loading false
                 :data []
                 :total 0
                 :pageSize 50
                 :pageNumber 1})

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
;      (js/console.log row-from-datagrid row)
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

(defn datagrid-toolbar []
  (let [filters (rf/subscribe [:patients/data-filters])
        selection (rf/subscribe [:patients/selection])]
  (r/as-element [:div
                 [:> LinkButton {:style {:margin "5px"}
                                 :iconCls "icon-add"
                                 :onClick

;                                 #(rf/dispatch [::some-event-in-patients-ns 2 2])}

                                 
                                        #(rf/dispatch [::show-dialog :create]) } "Add"
                                 ]
                 [:> LinkButton {:style {:margin "5px"}
                                 :iconCls "icon-reload"
                                 :onClick #(rf/dispatch [::patients-reload]) } "Reload"]
                 [:> LinkButton {:disabled (not @selection)
                                 :style {:margin "5px"}
                                 :iconCls "icon-cancel"
                                 :onClick #(rf/dispatch [:patients/show-dialog :delete]) } "Delete"]
                 [:> LinkButton {:disabled (not @selection)
                                 :style {:margin "5px"}
                                 :iconCls "icon-edit"
                                 :onClick #(rf/dispatch [:patients/show-dialog :update]) } "Update"]
                 [:> LinkButton {:style {:margin "5px"}
                                 :iconCls "icon-filter"} "Filter"]
                 [:> SearchBox {:style {:float "right" :margin "5px" :width "350px"}
                                :value (:text-like @filters)
                                :onChange #(rf/dispatch [:patients/update-data-filters [:text-like %]])}]])))

(rf/reg-event-fx :patients/read
  (fn [cofx [_ data]]
    (-> cofx
        (assoc-in [:db :patients :data] data)
        (assoc :dispatch [:patients/clear-data-filtered]))))


(defn entry []
  (let [data (rf/subscribe [:patients/data-filtered])
        filters (rf/subscribe [:patients/data-filters])
        selection (rf/subscribe [:patients/selection])]
;  (js/console.log "Data in table: " (count @data) @data (str @data))
  [:div
   [:> DataGrid {:data @data
                 :style {:height "100%"}
                 :selectionMode "single"
                 :toolbar datagrid-toolbar
                 :selection selection
                 :idField "uuid"
                 :virtualScroll true
                 :lazy true
                 :onPageChange (fn [a b c d] (js/console.log "onPageChange" a b c d))
                 :onRowClick #(rf/dispatch [:patients/on-selection-change [%]])} 
    [:> GridColumn {:width "30px" :align "center" :title "#"
                    :render #(inc (.-rowIndex %))}]
    [:> GridColumn {:width "250px" :title "Patient name"
                    :render #(-> % .-row (aget "resource") (aget "patient_name")
                            (column-render-text-like (:text-like @filters)))}]
    [:> GridColumn {:width "120px" :align "center" :title "Birth date"
                    :render #(-> % .-row (aget "resource") (aget "birth_date") timestamp->human-date
                                 (column-render-text-like (:text-like @filters)))}]
    [:> GridColumn {:width "70px" :title "Gender"
                    :render #(-> % .-row (aget "resource") (aget "gender")
                                 (column-render-text-like (:text-like @filters)))}]
    [:> GridColumn {:width "50%" :title "Address"
                    :render #(-> % .-row (aget "resource") (aget "address")
                                 (column-render-text-like (:text-like @filters)))}]
    [:> GridColumn {:width "50%"  :title "Policy number"
                    :render #(-> % .-row (aget "resource") (aget "policy_number")
                                 (column-render-text-like (:text-like @filters)))}]]]))

