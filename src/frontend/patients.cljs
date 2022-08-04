(ns frontend.patients
  (:require
   ["rc-easyui" :refer [Layout LayoutPanel DataGrid GridColumn LinkButton Dialog Form TextBox DateBox SearchBox MaskedBox ComboBox FormField ButtonGroup]]
   [reagent.core :as r]
   [clojure.string :refer [trim replace blank?]]
   [common.patients]
   [frontend.rf-nru-nwd :refer [reg-sub]]

   [frontend.patients.datagrid :as datagrid]
   [frontend.patients.filter-panel :as filter-panel]
   [frontend.patients.dialog-create :as dialog-create]
   [frontend.patients.dialog-update :as dialog-update]
   [frontend.patients.dialog-delete :as dialog-delete]
   [frontend.comm :as comm]
   [frontend.patients.models :as models]
   [re-frame.core :as rf]))

(def init-state {:show-dialog nil
                 :show-filter-panel false
                 :. {:datagrid datagrid/init-state
                     :filter-panel filter-panel/init-state
                     :dialog-update dialog-update/init-state
                     :dialog-create dialog-create/init-state
                     :dialog-delete dialog-delete/init-state}})

(def locales {:en {:human-date-format "MM/dd/yyyy"
                   :gender.male "Male"
                   :gender.female "Female"                   
                   :patient-name "Patient name"
                   :gender "Gender"
                   :birth-date "Birth date"
                   :policy-number "Policy number"
                   :address "Address"
                   :action.add "Add"
                   :action.reload "Reload"
                   :action.filter "Filter"
                   :action.delete "Delete"
                   :action.update "Update"
                   :filter.patient-name "Patient name contains:"
                   :filter.address "Address contains:"
                   :filter.gender "Gender:"
                   :filter.gender.any "Any"
                   :filter.gender.male "Male"
                   :filter.gender.female "Female"
                   :filter.policy-number "Policy number"
                   :filter.birth-date "Birth date"
                   :filter.birth-date.start "Start:"
                   :filter.birth-date.end "End:"                   
                   :filter.birth-date.any "Any"
                   :filter.birth-date.equal "Equal"
                   :filter.birth-date.after "After"
                   :filter.birth-date.before "Before"
                   :filter.birth-date.between "Between"                   
                   :filter.reset "Reset"
                   :filter.apply "Apply"
                   :dialog-create.caption "Create patient"
                   :dialog-create.button-create "Create"
                   :dialog-update.caption "Update patient"
                   :dialog-update.button-update "Update"
                   :dialog-delete {:caption "Delete patient"
                                   :text "Delete patient"
                                   :yes "Yes"
                                   :no "No"}}
              :ru {:human-date-format "dd.MM.yyyy"
                   :gender.male "Мужской"
                   :gender.female "Женский"
                   :patient-name "ФИО пациента"
                   :gender "Пол"
                   :birth-date "Дата рождения"
                   :policy-number "Номер полиса ОМС"
                   :address "Адрес"
                   :action.add "Добавить"
                   :action.reload "Обновить"
                   :action.filter "Фильтр"
                   :action.delete "Удалить"
                   :action.update "Изименить"
                   :filter.patient-name "ФИО пациента содержит:"
                   :filter.address "Адрес содержит:"
                   :filter.gender "Пол:"
                   :filter.gender.any "Любой"  
                   :filter.gender.male "Мужской"
                   :filter.gender.female "Женский"                   
                   :filter.policy-number "Полис ОМС:"                   
                   :filter.birth-date "Дата рождения:"
                   :filter.birth-date.start "От:"
                   :filter.birth-date.end "До:"                   
                   :filter.birth-date.any "Любая"     
                   :filter.birth-date.equal "Точно"
                   :filter.birth-date.after "После"
                   :filter.birth-date.before "До"
                   :filter.birth-date.between "Между"                                      
                   :filter.reset "Сброс"
                   :filter.apply "Применить"
                   :dialog-create.caption "Добавить"
                   :dialog-create.button-create "Добавить"
                   :dialog-update.caption "Обновить данные пациента"
                   :dialog-update.button-update "Сохранить"
                   :dialog-delete {:caption "Удалить запись"
                                   :text "Удалить пациента"
                                   :yes "Да"
                                   :no "Нет"}}})


(reg-sub ::state #(-> %))

(rf/reg-event-db ::show-filter-panel
  (fn [state [_ v]]
    (case v
      :close (assoc state :show-filter-panel false)
      :open  (assoc state :show-filter-panel true)
      nil (assoc state :show-filter-panel
                 (not (:show-filter-panel state))))))
    
(defn ui []
  (let [local-lang @(rf/subscribe [:locale-lang])
        locale (local-lang locales)
        state @(rf/subscribe [::state])
        selection (get-in state [:. :datagrid :selection])
        {:keys [show-filter-panel]} state]
  [:> Layout {:style {"width" "100%" "height" "100%"}}

      [:> LayoutPanel {:region "north" :style {"height" "60px"}}
       [:div {:style {:text-align "center"}}
        [:h1 {:style {:margin "10px"}}
         "Patients CRUD application"]]

       [:> ButtonGroup {:selectionMode "single"
                        :style {:position "absolute"
                                :top "15px"
                                :left "10px"}}

        [:> LinkButton {:iconCls "ru"
                        :selected (= local-lang :ru)
                        :onClick #(rf/dispatch [:switch-local-lang :ru])}]
        
        [:> LinkButton {:iconCls "en"
                        :selected (= local-lang :en)
                        :onClick #(rf/dispatch [:switch-local-lang :en])}]]]
   
   
       [:> LayoutPanel {:region "west"
                        :title (:action.filter locale)
                        :collapsible true
                        :expander true
                        :onExpand #(rf/dispatch [::show-filter-panel :open])
                        :onCollapse #(rf/dispatch [::show-filter-panel :close])                        
                        :collapsed (not show-filter-panel)
                        :style {:width "305px"}}
        [filter-panel/entry locale]]

   [:> LayoutPanel {:region "center" :style {:height "100%"}}
    [datagrid/entry locale state]
    [dialog-delete/entry selection (select-keys locale [:dialog-delete :human-date-format])]
    [dialog-create/entry locale models/Patient]
    [dialog-update/entry locale models/Patient]]]))
