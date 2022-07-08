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
  [_ v] [:like "patient_name" (str "%" v "%")])

(defmethod filter->where-cond :address
  [_ v] [:like "address" (str "%" v "%")])

(defmethod filter->where-cond :policy-number
  [_ v] [:= "policy_number" v])

(defmethod filter->where-cond :gender
  [_ v] [:= "gender" (v {:male "male" :female "female"})])

(defmethod filter->where-cond :birth-date
  [_ {:keys [mode start end]}]
    (let [dt->ts #(/ (.getTime %) 1000)]
      (case mode
        :equal   [:=  "birth_date" (dt->ts start)]
        :after   [:=> "birth_date" (dt->ts start)]
        :before  [:=< "birth_date" (dt->ts start)]
        :between [:between "birth_date" (dt->ts start) (dt->ts end)])))
  
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
      
(defn entry []
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
      "Patient name contains:"]
     [:> TextBox {:style input-style
                  :value patient-name
                  :onChange #(rf/dispatch [::update-filters [:patient-name] %])}]

     [:p {:style {:font-weight (when (not (blank? address)) "bold")}}
      "Address contains:"]
     [:> TextBox {:style input-style
                  :value address
                  :onChange #(rf/dispatch [::update-filters [:address] %])}]

     [:p {:style {:font-weight (when (not (blank? policy-number)) "bold")}}
      "Policy number equal:"]
     [:> TextBox {:style input-style
                  :value policy-number
                  :onChange #(rf/dispatch[::update-filters [:policy-number] %])}]

     [:p {:style {:font-weight (when (not (blank? gender)) "bold")}}
      "Gender:"]
     
    [:> ButtonGroup {:selectionMode "single"}
     [:> LinkButton {:selected (nil? gender)
                     :onClick #(rf/dispatch [::update-filters [:gender] nil])} "Any"]
     [:> LinkButton {:selected (= :male gender)
                     :onClick #(rf/dispatch [::update-filters [:gender] :male])}  "Male"]
     [:> LinkButton {:selected (= :female gender)
                     :onClick #(rf/dispatch [::update-filters [:gender] :female])} "Female"]]

     [:p {:style {:font-weight (when (not (blank? birth-date)) "bold")}}
      "Birth date:"]
     
    [:> ButtonGroup {:selectionMode "single"}
     [:> LinkButton {:selected (nil? birth-date-mode)
                     :onClick #(rf/dispatch [::update-filters [:birth-date] nil])} "Any"]
     [:> LinkButton {:selected (= birth-date-mode :equal)
                     :onClick #(rf/dispatch [::update-filters [:birth-date :mode] :equal])} "Equal"]
     [:> LinkButton {:selected (= birth-date-mode :after)
                     :onClick #(rf/dispatch [::update-filters [:birth-date :mode] :after])} "After"]
     [:> LinkButton {:selected (= birth-date-mode :before)
                     :onClick #(rf/dispatch [::update-filters [:birth-date :mode] :before])} "Before"]
     [:> LinkButton {:selected (= birth-date-mode :between)
                     :onClick #(rf/dispatch [::update-filters [:birth-date :mode] :between])}"Between"]]

     (when (some #(= birth-date-mode %) '(:equal :after :before :between))
       [:p (when (= birth-date-mode :between) "Start: ")
        [:> DateBox {:style input-style
                     :value birth-date-start
                     :onChange #(rf/dispatch [::update-filters [:birth-date :start] %])}]])

     (when (= birth-date-mode :between)
       [:p "End: "
        [:> DateBox {:style input-style
                     :value birth-date-end
                     :onChange #(rf/dispatch [::update-filters [:birth-date :end] %])}]])
     [:hr]

     [:> LinkButton {:onClick #(rf/dispatch [::reset-filters])} "Reset"]
     [:> LinkButton {:style {:float "right"}
                     :onClick #(rf/dispatch [::apply-filters])} "Apply"]

     [:p (str @(rf/subscribe [::state])) ]]))


