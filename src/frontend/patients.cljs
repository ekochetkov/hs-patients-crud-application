(ns frontend.patients
  (:require
   ["rc-easyui" :refer [Layout
                        LayoutPanel
                        LinkButton
                        ButtonGroup]]
   [common.patients]
   [frontend.rf-nru-nwd :refer [reg-sub]]
   [frontend.patients.datagrid :as datagrid]
   [frontend.patients.search-panel :as search-panel]
   [common.ui-anchors.patients.core :as anchors] 
   [common.ui-anchors.core :refer [make-anchor]]   
   [frontend.patients.dialog-create :as dialog-create]
   [frontend.patients.dialog-update :as dialog-update]
   [frontend.patients.dialog-delete :as dialog-delete]
   [frontend.patients.models :as models]
   [re-frame.core :as rf]))

(def init-state {:show-dialog nil
                 :show-search-panel false
                 :. {:datagrid datagrid/init-state
                     :search-panel search-panel/init-state
                     :dialog-update dialog-update/init-state
                     :dialog-create dialog-create/init-state
                     :dialog-delete dialog-delete/init-state}})

(def locales {:en {:datagrid {:displayMsg "Displaying {from} to {to} of {total} items"
                              :loadMsg "Processing... Please wait"
                              :use-context-menu-message "Use context menu for update or delete row"
                              :no-rows-message "Please, change search or filtering criteria and try again"
                              :no-rows-title "No data for display"}
                   :human-date-format "MM/dd/yyyy"
                   :calendarOptions {:defaultWeeks ["S" "M" "T" "W" "T" "F" "S"]
                                     :firstDay 0
                                     :defaultCurrentText "Today"
                                     :defaultCloseText "Close"
                                     :defaultMonths ["Jan" "Feb" "Mar" "Apr"
                                                     "May" "Jun" "Jul" "Aug"
                                                     "Sep" "Oct" "Nov" "Dec"]}
                   :gender.male "Male"
                   :gender.female "Female"                   
                   :patient-name "Patient name"
                   :gender "Gender"
                   :birth-date "Birth date"
                   :policy-number "Policy number"
                   :address "Address"
                   :action.add "Add"
                   :action.reload "Reload"
                   :action.search "Search"
                   :action.delete "Delete"
                   :action.update "Update"
                   :search.patient-name "Patient name contains:"
                   :search.address "Address contains:"
                   :search.gender "Gender:"
                   :search.gender.any "Any"
                   :search.gender.male "Male"
                   :search.gender.female "Female"
                   :search.policy-number "Policy number"
                   :search.birth-date "Birth date:"
                   :search.birth-date.start "Start:"
                   :search.birth-date.end "End:"                   
                   :search.birth-date.any "Any"
                   :search.birth-date.equal "Equal"
                   :search.birth-date.after "After"
                   :search.birth-date.before "Before"
                   :search.birth-date.between "Between"                   
                   :search.reset "Reset"
                   :search.apply "Apply"
                   :validate-rule-required "This field is mandatory"
                   :validate-rule-message {:double-policy_number "A patient with this policy number already exists"
                                           :patient_name "The name can only contain characters and digits longer than 5"
                                           :policy_number "The policy number contains 16 digits"}
                   :dialog-create.caption "Create patient"
                   :dialog-create.button-create "Create"
                   :dialog-update.caption "Update patient"
                   :dialog-update.button-update "Update"
                   :dialog-delete {:caption "Delete patient"
                                   :text "Delete patient"
                                   :yes "Yes"
                                   :no "No"}}

              
              :ru {:datagrid {:displayMsg "Показаны записи с {from} по {to}. Всего {total}."
                              :loadMsg "Обработка... Пожалуйста, подождите"
                              :use-context-menu-message "Используйте контекстное меню для обновления или удаления строки"
                              :no-rows-message "Пожалуйста, измените критерии поиска или фильтрации и повторите попытку"
                              :no-rows-title "Нет данных для отображения"}                              
                   :human-date-format "dd.MM.yyyy"
                   :calendarOptions {:defaultWeeks ["ВС" "ПН" "ВТ" "СР" "ЧТ" "ПТ" "СБ"]
                                     :firstDay 1
                                     ;; Bug in easy-ui component
                                     ;; can't replace current and close text
                                     :defaultCurrentText "Сегодня"
                                     :defaultCloseText "Закрыть"
                                     :defaultMonths ["Январь"  "Февраль" "Март"
                                                     "Апрель"  "Май"     "Июнь"
                                                     "Июль"    "Август"  "Сентябрь"
                                                     "Октябрь" "Ноябрь"  "Декабрь"]}
                   :gender.male "Мужской"
                   :gender.female "Женский"
                   :patient-name "ФИО пациента"
                   :gender "Пол"
                   :birth-date "Дата рождения"
                   :policy-number "Номер полиса ОМС"
                   :address "Адрес"
                   :action.add "Добавить"
                   :action.reload "Обновить"
                   :action.search "Поиск"
                   :action.delete "Удалить"
                   :action.update "Изименить"
                   :search.patient-name "ФИО пациента содержит:"
                   :search.address "Адрес содержит:"
                   :search.gender "Пол:"
                   :search.gender.any "Любой"  
                   :search.gender.male "Мужской"
                   :search.gender.female "Женский"                   
                   :search.policy-number "Полис ОМС:"                   
                   :search.birth-date "Дата рождения:"
                   :search.birth-date.start "От:"
                   :search.birth-date.end "До:"                   
                   :search.birth-date.any "Любая"     
                   :search.birth-date.equal "Точно"
                   :search.birth-date.after "После"
                   :search.birth-date.before "До"
                   :search.birth-date.between "Между"                                      
                   :search.reset "Сброс"
                   :search.apply "Применить"
                   :validate-rule-required "Это поле является обязательным"
                   :validate-rule-message {:double-policy_number "Пациент с данным номером полиса уже существует"
                                           :patient_name         "Имя может содержать только символы и цифры длиной более 5"
                                           :policy_number        "Номер полиса содержит 16 цифр"}
                   :dialog-create.caption "Добавить"
                   :dialog-create.button-create "Добавить"
                   :dialog-update.caption "Обновить данные пациента"
                   :dialog-update.button-update "Сохранить"
                   :dialog-delete {:caption "Удалить запись"
                                   :text "Удалить пациента"
                                   :yes "Да"
                                   :no "Нет"}}})


(reg-sub ::state #(-> %))

(rf/reg-event-db ::show-search-panel
  (fn [state [_ v]]
    (case v
      :close (assoc state :show-search-panel false)
      :open  (assoc state :show-search-panel true)
      nil (assoc state :show-search-panel
                 (not (:show-search-panel state))))))
    
(defn ui []
  (let [local-lang @(rf/subscribe [:locale-lang])
        locale (local-lang locales)
        state @(rf/subscribe [::state])
        selection (get-in state [:. :datagrid :selection])
        {:keys [show-search-panel]} state]
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
                        :title (:action.search locale)
                        :collapsible true
                        :expander true
                        :onExpand #(rf/dispatch [::show-search-panel :open])
                        :onCollapse #(rf/dispatch [::show-search-panel :close])                        
                        :collapsed (not show-search-panel)
                        :style {:width "305px"}}
        (make-anchor anchors/search-panel)
        [search-panel/entry locale]]

   [:> LayoutPanel {:region "center" :style {:height "100%"}}
    [datagrid/entry locale state]
    [dialog-delete/entry selection (select-keys locale [:dialog-delete :human-date-format])]
    [dialog-create/entry locale models/Patient]
    [dialog-update/entry locale models/Patient]]]))
