(ns frontend.patients.filter-panel
  (:require
   ["rc-easyui" :refer [LinkButton
                        TextBox
                        DateBox
                        MaskedBox
                        ButtonGroup]]
   [clojure.string :refer [blank?]]
   [common.patients]
   [frontend.patients.models :refer [Patient]]
   [common.ui-anchors.patients.search-panel :as anchors]
   [common.ui-anchors.core :as ui-anchors]
   [frontend.rf-nru-nwd :as rf-nru-nwd :refer [reg-sub]]   
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

(def kw->rc-easy-ui-class {:TextBox TextBox
                           :DateBox DateBox
                           :ButtonGroup ButtonGroup
                           :LinkButton LinkButton
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
             content]]
        )
      ]]
  )

;;(defn build-form-field-node [anchor rc-input-class rc-input-attrs & childs]
;;
;;  )

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
    [:div {:id anchors/form
           :style {:padding "10px"}}

     [:p {:style {:font-weight (when (not (blank? patient-name)) "bold")}}
      (:filter.patient-name locale)]

     [form-field "patient_name" :TextBox
      {:style input-style
       :value patient-name
       :onChange #(rf/dispatch [::update-filters [:patient-name] %])}]

     [:p {:style {:font-weight (when (not (blank? address)) "bold")}}
      (:filter.address locale)]
     
     [form-field "address" :TextBox
      {:style input-style
       :value address
       :onChange #(rf/dispatch [::update-filters [:address] %])}] 

     [:p {:style {:font-weight (when (not (blank? policy-number)) "bold")}}
      (:filter.policy-number locale)]
     
     [form-field "policy_number" :MaskedBox
      {:style input-style
       :mask "9999 9999 9999 9999"                    
       :value ((get-in Patient [:converts "policy_number" :get]) policy-number)
       :onChange #(rf/dispatch[::update-filters [:policy-number]
                            ((get-in Patient [:converts "policy_number" :set]) %)
                               ])}]

     [:p {:style {:font-weight (when (not (blank? gender)) "bold")}}
      (:filter.gender locale)]

     [form-field "gender" :ButtonGroup
      {:selectionMode "single"}      
        ["any" :LinkButton {:selected (nil? gender)
                            :onClick #(rf/dispatch [::update-filters [:gender] nil])}
         (:filter.gender.any locale)]

        ["male" :LinkButton {:selected (= :male gender)
                             :onClick #(rf/dispatch [::update-filters [:gender] :male])}
         (:filter.gender.male locale)]

        ["female" :LinkButton {:selected (= :female gender)
                               :onClick #(rf/dispatch [::update-filters [:gender] :female])}
         (:filter.gender.female locale)]]

     [:p {:style {:font-weight (when (not (blank? birth-date)) "bold")}}
      (:filter.birth-date locale)]

     [form-field "birth_date_mode" :ButtonGroup
      {:selectionMode "single"}
      ["any" :LinkButton {:selected (nil? birth-date-mode)
                          :onClick #(rf/dispatch [::update-filters [:birth-date] nil])}
       (:filter.birth-date.any locale)]

      ["equal" :LinkButton {:selected (= birth-date-mode :equal)
                            :onClick #(rf/dispatch [::update-filters [:birth-date :mode] :equal])}
       (:filter.birth-date.equal locale)]

      ["after" :LinkButton {:selected (= birth-date-mode :after)
                           :onClick #(rf/dispatch [::update-filters [:birth-date :mode] :after])}
       (:filter.birth-date.after locale)] 

       ["before" :LinkButton {:selected (= birth-date-mode :before)
                              :onClick #(rf/dispatch [::update-filters [:birth-date :mode] :before])}
        (:filter.birth-date.before locale)]

       ["between" :LinkButton {:selected (= birth-date-mode :between)
                               :onClick #(rf/dispatch [::update-filters [:birth-date :mode] :between])}
        (:filter.birth-date.between locale)]]

     (when (some #(= birth-date-mode %) '(:equal :after :before :between))
       [:p (when (= birth-date-mode :between) (:filter.birth-date.start locale))
        [form-field "birth_date_start" :DateBox
         {:style input-style
          :value birth-date-start
          :format (:human-date-format locale)                     
          :onChange #(rf/dispatch [::update-filters [:birth-date :start] %])}]])

     (when (= birth-date-mode :between)
       [:p (:filter.birth-date.end locale)
        [form-field "birth_date_end" :DateBox
         {:style input-style
                     :value birth-date-end
                     :format (:human-date-format locale)
                     :onChange #(rf/dispatch [::update-filters [:birth-date :end] %])}]])
     [:hr]

     [ui-anchors/make-anchor anchors/reset-button
      [:> LinkButton {:onClick #(rf/dispatch [::reset-filters])} (:filter.reset locale)]]
     
     [ui-anchors/make-anchor anchors/apply-button
      [:> LinkButton {:style {:float "right"}
                     :onClick #(rf/dispatch [::apply-filters])} (:filter.apply locale)]]]))


