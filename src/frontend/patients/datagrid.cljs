(ns frontend.patients.datagrid
  (:require
   ["rc-easyui" :refer [MenuSep DataGrid GridColumn LinkButton SearchBox Menu MenuItem LinkButton
                        TextBox
                        DateBox
                        MaskedBox
                        ButtonGroup]]
   [reagent.core :as r]
   [common.ui-anchors.core :as ui-anchors]
   [clojure.string :refer [join]]
   [common.patients]
   [common.ui-anchors.patients.datagrid :as anchors]
   [frontend.rf-nru-nwd :as rf-nru-nwd :refer [reg-sub]]   
   [frontend.utils :refer [ts->human-date]]
   [frontend.comm :as comm]
   [re-frame.core :as rf]))

(def init-state {:filter-text-like nil
                 :selection nil                 
                 :loading false
                 :show-context-menu {:show false
                                     :position {:x nil :y nil}}
                 :where []
                 :data []
                 :total -1
                 :page-size 35
                 :page-number 1})

(reg-sub ::state #(-> %))

(rf/reg-event-db ::update-filter-text-like
  (fn [state [_ value]] (assoc state :filter-text-like value)))

(rf/reg-event-fx ::update-where
  (fn [cofx [_ where]]
    (-> {:db (:db cofx)}
         (assoc-in [:db :where] where)
         (assoc-in [:db :data] [])
         (assoc-in [:db :page-number] 1)
         (assoc-in [:db :total] 0)
         (assoc :fx [[:dispatch [::datagrid-reload]]]))))

(rf/reg-event-fx ::datagrid-reload
  (fn [cofx]
    (let [{:keys [where page-number page-size]} (:db cofx)]
      (-> {:db (:db cofx)}
        (assoc-in [:db :loading] true)
        (assoc :fx [[:dispatch [::comm/send-event ::read [:patients/read {:where       where
                                                                          :page-number page-number
                                                                          :page-size   page-size}]]]])))))

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
    (-> {:db (:db cofx)}
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

(def context-menu-items
  (list
   {:type :MenuItem
    :id "anchor-update"
    :rc-attrs (fn [locale] {:text (:action.update locale)
                            :value "update"
                            :iconCls "icon-edit"})}
   {:type :MenuSep}
   {:type :MenuItem    
    :id "anchor-delete"
    :rc-attrs (fn [locale] {:text (:action.delete locale)
                            :value "delete"
                            :iconCls "icon-cancel"})}))

(defn context-menu [conf locale]
  [:span {:id "dds" :items "s,d,f"    
          :onMouseLeave #(rf/dispatch [::on-hide-context-menu])}
   (into [:> Menu {:inline true
                   :attrs {:id "ghgh"}
            :onItemClick #(rf/dispatch [::on-context-menu-item-click %])
            :style {:position "fixed"
                    :left (-> conf :position :x)
                    :top  (-> conf :position :y)}}]
        (map #(case (:type %)
                :MenuItem [:> MenuItem ((:rc-attrs %) locale)]
                :MenuSep [:> MenuSep])) context-menu-items)])       

(rf/reg-event-fx ::on-page-change
  (fn [cofx [_ page-number page-size]]
      (-> {:db (:db cofx)}
        (assoc-in [:db :page-number] page-number)
        (assoc-in [:db :page-size] page-size)
        (assoc :fx [[:dispatch [::datagrid-reload]]]))))

(defn on-page-change [event]
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

(def kw->rc-easy-ui-class {:TextBox TextBox
                           :DateBox DateBox
                           :ButtonGroup ButtonGroup
                           :LinkButton LinkButton
                           :SearchBox SearchBox
                           :MaskedBox MaskedBox})

(defn rc-input-class->fieldtype
  [rc-input-class rc-input-attrs]
     (case rc-input-class
        :TextBox (if (:multiline rc-input-attrs)
                        "TextArea"
                        "TextBox")
        rc-input-class))

(defn form-field [name rc-input-class rc-input-attrs & childs]
  [:span {:id name
          :type "field"
          :fieldtype (rc-input-class->fieldtype rc-input-class
                                                rc-input-attrs)}
     [:> (kw->rc-easy-ui-class rc-input-class)
      rc-input-attrs
      (for [[sub-value
             sub-rc-input-class
             sub-rc-input-attrs
             content] childs]

        [:span {:type "subfield"
                :value sub-value}
         [:> (kw->rc-easy-ui-class sub-rc-input-class)
             sub-rc-input-attrs
             content]])]])

(defn datagrid-toolbar [locale state parent-state]
  (let [{:keys [filter-text-like loading]} state
        {:keys [show-search-panel]} parent-state]
  (r/as-element
   [:div
    ;; Add
    [ui-anchors/make-anchor anchors/toolbar-patient-add-button
      [:> LinkButton {:iconCls "icon-add" :style {:margin "5px" :width "100px"}
                      :onClick #(rf/dispatch [:frontend.patients.dialog-create/show-dialog])}
       (:action.add locale)]]
    ;; Reload
    [ui-anchors/make-anchor anchors/toolbar-reload-page
     [:> LinkButton {:iconCls "icon-reload" :style {:margin "5px" :width "100px"}
                     :disabled loading
                     :onClick #(rf/dispatch [::datagrid-reload])}
      (:action.reload locale)]]
    ;; Search
    [ui-anchors/make-anchor anchors/toolbar-search-button
     [:> LinkButton {:iconCls "icon-search" :style {:margin "5px" :width "100px"}
                     :toggle true
                     :selected show-search-panel
                     :onClick #(rf/dispatch [:frontend.patients/show-search-panel])}
      (:action.filter locale)]]
    ;; Filter
    [ui-anchors/make-anchor anchors/toolbar-filter-box
     [form-field "filter_box" :SearchBox
      {:style {:float "right" :margin "5px" :width "350px"}
       :value filter-text-like
       :buttonIconCls "icon-filter" 
       :onChange #(rf/dispatch [::update-filter-text-like %])}]]

    [:br]
    [:p {:style {:margin "2 2 2 5"
                 :font-size "small"}}
     (-> locale :datagrid :use-context-menu-message)]])))

(defn entry [locale parent-state]
  (let [state             @(rf/subscribe [::state])
        {:keys [selection
                total
                page-size
                page-number
                loading
                data]}    state
        rows-after-filter (data-view data (:filter-text-like state) locale)
        rows-in-datagrid  (if (empty? rows-after-filter)
                            [{"no_rows_message" (-> locale :datagrid :no-rows-message)}]
                            rows-after-filter)]
    [:div {:id     anchors/datagrid-table
           :fields "#,patient_name,birth_date,gender,policy_number,address"}
     (into [:> DataGrid {:data             rows-in-datagrid
                         :style            {:height "100%"}
                         :selectionMode    "single"
                         :toolbar          (partial datagrid-toolbar locale state parent-state)
                         :selection        selection
                         :idField          "uuid"
                         :pageSize         page-size
                         :total            total
                         :pageNumber       page-number
                         :loading          loading
                         :defaultLoadMsg   (get-in locale [:datagrid :loadMsg])
                         :pagination       true
                         :lazy             true
                         :pagePosition     "bottom"
                         :pageOptions      {:layout ["list" "sep" "first"
                                                     "prev" "sep" "tpl"
                                                     "sep" "next" "last"
                                                     "sep" "refresh" "info" "links"]
                                            :displayMsg (get-in locale [:datagrid :displayMsg])
                                            :pageList   [35 100]}
                         :onRowContextMenu (fn [rowEvent]
                                             (-> rowEvent .-originalEvent .preventDefault)
                                             (rf/dispatch [::on-row-context-menu rowEvent]))
                         :onRowClick       (fn [datagrid-row] (rf/dispatch [::on-row-click datagrid-row]))
                         :onPageChange     on-page-change}]

           (if (-> rows-in-datagrid
                   first
                   (get "no_rows_message"))
             [[:> GridColumn {:width "40px" :title (-> locale :datagrid :no-rows-title) :align "center" :field "no_rows_message"}]]
             [[:> GridColumn {:width "40px" :title "#" :align "center" :render #(inc (.-rowIndex %))}]
              [:> GridColumn {:width "400px" :field "patient_name"
                              :title (:patient-name locale)}]
              [:> GridColumn {:width "140px" :align "center" :field "birth_date"
                              :title (:birth-date locale)}]
              [:> GridColumn {:width "100px" :field "gender" :align "center"
                              :title (:gender locale)}]
              [:> GridColumn {:width "220px" :align "center" :field "policy_number"
                              :title (:policy-number locale)}]
              [:> GridColumn {:width "100%" :field "address"
                              :title (:address locale) }]]))
     (when (-> state :show-context-menu :show)
       [context-menu (-> state :show-context-menu) locale])]))
