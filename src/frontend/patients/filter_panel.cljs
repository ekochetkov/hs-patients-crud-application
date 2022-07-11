(ns frontend.patients.filter-panel
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
   [re-frame.core :as rf]))

(def init-state {:filters {}})

(reg-sub ::state #(-> %))

(rf/reg-event-db ::update-filters
  (fn [state [_ field-path value]]
    (let [full-path (concat [:filters] field-path)]
      (if (blank? value)
        (update-in state (drop-last full-path) dissoc (last full-path))
        (assoc-in state full-path value)))))

(defmulti filter->where-cond
  (fn [k _] k))

(defmethod filter->where-cond :patient-name
  [_ v] [:ilike "patient_name" (str "%" v "%")])

(defmethod filter->where-cond :address
  [_ v] [:ilike "address" (str "%" v "%")])

(defmethod filter->where-cond :policy-number
  [_ v] [:= "policy_number" v])

(defmethod filter->where-cond :gender
  [_ v] [:= "gender" (v {:male "male" :female "female"})])

(defn- js-date->ts-without-tz [js-date]
  (- (.getTime js-date)
     (* (.getTimezoneOffset js-date) 60000)))

(defmethod filter->where-cond :birth-date
  [_ {:keys [mode start end]}]
    (case mode
      :equal   [:=  "birth_date" (js-date->ts-without-tz start)]
      :after   [:>= "birth_date" (js-date->ts-without-tz start)]
      :before  [:<= "birth_date" (js-date->ts-without-tz start)]
      :between [:between "birth_date" (js-date->ts-without-tz start)
                                      (js-date->ts-without-tz end)]))
  
(defn- build-where-by-filters [filters]
  (->> filters
       (mapv (fn [[k v]] (filter->where-cond k v)))))

(rf/reg-event-fx ::reset-filters
   (fn [cofx]
     (-> cofx
        (assoc-in [:db :filters] {})
        (assoc :fx [[:dispatch [:frontend.patients.datagrid/update-where []]]]))))


(rf/reg-event-fx ::apply-filters
  (fn [cofx]
    (let [filters (get-in cofx [:db :filters])
          where (build-where-by-filters filters)]
      (assoc cofx :fx
        [[:dispatch [:frontend.patients.datagrid/update-where where]]]))))
      
(defn entry [locale]
  (let [state @(rf/subscribe [::state])
        {:keys [patient-name
                address
                policy-number
                gender
                birth-date]} (:filters state)
        {birth-date-mode  :mode
         birth-date-start :start
         birth-date-end   :end} birth-date
        input-style {:width "100%"}]
    [:div {:style {:padding "10px"}}

     [:p {:style {:font-weight (when (not (blank? patient-name)) "bold")}}
      (:filter.patient-name locale)]
     [:> TextBox {:style input-style
                  :value patient-name
                  :onChange #(rf/dispatch [::update-filters [:patient-name] %])}]

     [:p {:style {:font-weight (when (not (blank? address)) "bold")}}
      (:filter.address locale)]
     [:> TextBox {:style input-style
                  :value address
                  :onChange #(rf/dispatch [::update-filters [:address] %])}]

     [:p {:style {:font-weight (when (not (blank? policy-number)) "bold")}}
      (:filter.policy-number locale)]
     [:> MaskedBox {:style input-style
                    :mask "9999 9999 9999 9999"                    
                    :value policy-number
                    :onChange #(rf/dispatch[::update-filters [:policy-number] %])}]

     [:p {:style {:font-weight (when (not (blank? gender)) "bold")}}
      (:filter.gender locale)]
     
    [:> ButtonGroup {:selectionMode "single"}
     [:> LinkButton {:selected (nil? gender)
                     :onClick #(rf/dispatch [::update-filters [:gender] nil])} (:filter.gender.any locale)]
     [:> LinkButton {:selected (= :male gender)
                     :onClick #(rf/dispatch [::update-filters [:gender] :male])} (:filter.gender.male locale)]
     [:> LinkButton {:selected (= :female gender)
                     :onClick #(rf/dispatch [::update-filters [:gender] :female])} (:filter.gender.female locale)]]

     [:p {:style {:font-weight (when (not (blank? birth-date)) "bold")}}
      (:filter.birth-date locale)]
     
    [:> ButtonGroup {:selectionMode "single"}
     [:> LinkButton {:selected (nil? birth-date-mode)
                     :onClick #(rf/dispatch [::update-filters [:birth-date] nil])} (:filter.birth-date.any locale)]
     [:> LinkButton {:selected (= birth-date-mode :equal)
                     :onClick #(rf/dispatch [::update-filters [:birth-date :mode] :equal])} (:filter.birth-date.equal locale)]
     [:> LinkButton {:selected (= birth-date-mode :after)
                     :onClick #(rf/dispatch [::update-filters [:birth-date :mode] :after])} (:filter.birth-date.after locale)]
     [:> LinkButton {:selected (= birth-date-mode :before)
                     :onClick #(rf/dispatch [::update-filters [:birth-date :mode] :before])} (:filter.birth-date.before locale)]
     [:> LinkButton {:selected (= birth-date-mode :between)
                     :onClick #(rf/dispatch [::update-filters [:birth-date :mode] :between])} (:filter.birth-date.between locale)]]

     (when (some #(= birth-date-mode %) '(:equal :after :before :between))
       [:p (when (= birth-date-mode :between) (:filter.birth-date.start locale))
        [:> DateBox {:style input-style
                     :value birth-date-start
                     :onChange #(rf/dispatch [::update-filters [:birth-date :start] %])}]])

     (when (= birth-date-mode :between)
       [:p (:filter.birth-date.end locale)
        [:> DateBox {:style input-style
                     :value birth-date-end
                     :onChange #(rf/dispatch [::update-filters [:birth-date :end] %])}]])
     [:hr]

     [:> LinkButton {:onClick #(rf/dispatch [::reset-filters])} (:filter.reset locale)]
     [:> LinkButton {:style {:float "right"}
                     :onClick #(rf/dispatch [::apply-filters])} (:filter.apply locale)]
     ]))


