(ns frontend.patients.datagrid
  (:require
   ["rc-easyui" :refer [MenuSep dateHelper Layout LayoutPanel DataGrid GridColumn LinkButton Dialog Form TextBox DateBox SearchBox MaskedBox ComboBox FormField ButtonGroup Menu MenuItem]]
   [reagent.core :as r]
   [clojure.string :refer [trim replace blank? join]]
   [frontend.modules :as rfm]
   [common.patients]
   [clojure.string :refer [trim replace blank?]]  
   [frontend.rf-nru-nwd :as rf-nru-nwd :refer [reg-sub]]   
;   [frontend.patients :refer [ui-patients-model]]
   [frontend.patients.models :as models]
   [frontend.utils :refer [with-id ts->human-date]]
   [frontend.comm :as comm]
   [re-frame.core :as rf :refer [trim-v]]))

(def init-state {:filter-text-like nil
                 :selection nil                 
                 :loading false
                 :show-context-menu {:show false
                                     :position {:x nil :y nil}}
                 :where []
                 :data []
                 :total 0
                 :page-size 50
                 :page-number 1})

(reg-sub ::state #(-> %))

(rf/reg-event-db ::update-filter-text-like
  (fn [state [_ value]] (assoc state :filter-text-like value)))

(rf/reg-event-fx ::update-where
  (fn [cofx [_ where]]
     (-> cofx
         (assoc-in [:db :where] where)
         (assoc-in [:db :data] [])
         (assoc-in [:db :page-number] 1)
         (assoc-in [:db :total] 0)
         (assoc :fx [[:dispatch [::datagrid-reload]]]))))

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
  (fn [state [_ [_ {:keys [total page-number page-size rows]}]]]
    (assoc state :data rows
                 :total total
                 :page-number page-number
                 :page-size page-size
                 :loading false)))

(defn- datagrid-row->data-row [row-from-datagrid data]
  (let [uuid (aget row-from-datagrid "uuid")]
    (first (filter #(= (:uuid %) uuid) data))))

(rf/reg-event-db ::on-row-context-menu
 (fn [state [_ rowEvent]]
  (let [datagrid-row (.-row rowEvent)
        original-event (.-originalEvent rowEvent)
        x (- (.-clientX original-event) 140)
        y (- (.-clientY original-event)  20)]
    (-> state
        (assoc :selection (datagrid-row->data-row datagrid-row (:data state)))
        (assoc :show-context-menu {:show true
                                   :position {:x (if (pos? x) x 0)
                                              :y (if (pos? y) y 0)}})))))

(rf/reg-event-fx ::on-context-menu-item-click
 (fn [cofx [_ value]]
   (let [selection (get-in cofx [:db :selection])]
    (-> cofx
      (assoc-in [:db :show-context-menu :show] false)
      (assoc :fx [[:dispatch
                   (case value
                     "update" [:frontend.patients.dialog-update/show-dialog selection]
                     "delete" [:frontend.patients.dialog-delete/show-dialog selection])]])))))

(rf/reg-event-db ::on-row-click
  (fn [state [_ datagrid-row]]
      (assoc state :selection (datagrid-row->data-row datagrid-row (:data state)))))

(rf/reg-event-db ::on-hide-context-menu
 (fn [state]
   (assoc-in state [:show-context-menu :show] false)))                   

(defn- context-menu [conf locale]
  [:span {:onMouseLeave #(rf/dispatch [::on-hide-context-menu])}
  [:> Menu {:inline true
            :onItemClick #(rf/dispatch [::on-context-menu-item-click %])
            :style {:position "fixed"
                    :left (-> conf :position :x)
                    :top  (-> conf :position :y)}}
   [:> MenuItem {:text (:action.update locale)
                 :value "update"
                 :iconCls "icon-edit"}]
   [:> MenuSep]
   [:> MenuItem {:text (:action.delete locale)
                 :value "delete"
                 :iconCls "icon-cancel"}]]])

(defn on-page-change [event]
  (js/console.log "onPageChange")
  (let [page-number (.-pageNumber event)
        page-size (.-pageSize event)]
  (rf/dispatch [::on-page-change page-number page-size])))

(defn- mapping-data-from-back [locale data]
  (->> data
    (map (fn [row]
      (assoc (:resource row)
          "gender" (case (get-in row [:resource "gender"])
                      "male" (:gender.male locale)
                      "female" (:gender.female locale)
                       "unknow")
                  "uuid" (:uuid row)
                  "policy_number" (->> (partition-all 4 (get-in row [:resource "policy_number"]))
                                       (map #(join %))
                                       (join " "))
                  "birth_date" (ts->human-date (get-in row [:resource "birth_date"]) (:human-date-format locale)))))))

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

(defn data-view [data filter-text-like locale]
      (-> data
         ((partial mapping-data-from-back locale))
         ((fn [data]
            (if-let [ftl filter-text-like]
              (let [pattern (js/RegExp. ftl "gi")]
                (-> data
                    ((partial filter-data pattern))
                    ((partial highlite-data pattern))))
              data)))))

(defn- toolbar-buttons [locale {:keys [selection filter-text-like loading]} {:keys [show-filter-panel]}]
  [{:id "patients-datagrid-toolbar-button-add"
    :caption (:action.add locale) :class :LinkButton :iconCls "icon-add" :style {:margin "5px" :width "100px"}
    :onClick #(rf/dispatch [:frontend.patients.dialog-create/show-dialog])}
    
   {:id "patients-datagrid-toolbar-button-reload"
    :caption (:action.reload locale) :class :LinkButton :iconCls "icon-reload" :style {:margin "5px" :width "100px"}
    :disabled loading
    :onClick #(rf/dispatch [::datagrid-reload])}

   {:id "patients-datagrid-toolbar-button-filter"
    :caption (:action.filter locale) :class :LinkButton :iconCls "icon-search" :style {:margin "5px" :width "100px"}
    :toggle true
    :selected show-filter-panel
    :onClick #(rf/dispatch [:frontend.patients/show-filter-panel])}
   
   {:class :SearchBox :style {:float "right" :margin "5px" :width "350px"}
    :value filter-text-like
    :buttonIconCls "icon-filter"
    :onChange #(rf/dispatch [::update-filter-text-like %])}])

(defn- datagrid-toolbar [locale state parent-state]
  (r/as-element (into [:div]
     (for [tb (toolbar-buttons locale state parent-state)]
       (with-id (:id tb)
        [:> (case (:class tb)
               :LinkButton LinkButton
               :SearchBox SearchBox) tb (:caption tb)])))))

(defn on-click [context-event row]
  (js/console.log context-event row))

(defn entry [locale parent-state]
  (let [state @(rf/subscribe [::state])
        data (:data state)
        {:keys [selection total page-size page-number loading]} state]
    [:div
     [:> DataGrid {:data (data-view data (:filter-text-like state) locale)
                   :style {:height "100%"}
                   :selectionMode "single"
                   :toolbar (partial datagrid-toolbar locale state parent-state)
                   :selection selection
                   :idField "uuid"
                   :pageSize page-size
                   :total total
                   :pageNumber page-number
                   :loading loading
                   :defaultLoadMsg (get-in locale [:datagrid :loadMsg])
                   :pagination true
                   :pagePosition "bottom"
                   :pageOptions {:layout ["list" "sep" "first" "prev" "sep" "tpl" "sep" "next" "last" "sep" "refresh" "info" "links"]
                                 :displayMsg (get-in locale [:datagrid :displayMsg])
                                 :pageList [10 50 100]}
                   :onRowContextMenu (fn [rowEvent]
                                       (-> rowEvent .-originalEvent .preventDefault)
                                       (rf/dispatch [::on-row-context-menu rowEvent]))
                   :onRowClick (fn [datagrid-row] (rf/dispatch [::on-row-click datagrid-row]))}
    
    [:> GridColumn {:width "40px"  :title "#" :align "center"  :render #(inc (.-rowIndex %))}]
    [:> GridColumn {:width "400px" :field "patient_name"
                    :title (:patient-name locale)}]
    [:> GridColumn {:width "140px" :align "center" :field "birth_date"
                    :title (:birth-date locale)}]
    [:> GridColumn {:width "100px"  :field "gender" :align "center"
                    :title (:gender locale)}]
    [:> GridColumn {:width "220px" :align "center" :field "policy_number"
                    :title (:policy-number locale)}]
    [:> GridColumn {:width "100%"  :field "address"
                    :title (:address locale) }]]
     (when (-> state :show-context-menu :show)
       [context-menu (-> state :show-context-menu) locale])
     ]))
