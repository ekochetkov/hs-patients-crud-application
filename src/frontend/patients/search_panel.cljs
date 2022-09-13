(ns frontend.patients.search-panel
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

(def init-state {:searches {}})

(reg-sub ::state #(-> %))

(rf/reg-event-db ::update-searches
  (fn [state [_ field-path value]]
    (let [full-path (concat [:searches] field-path)]
      (if (blank? value)
        (update-in state (drop-last full-path) dissoc (last full-path))
        (assoc-in state full-path value)))))

(defmulti search->where-cond
  (fn [k _] k))

(defmethod search->where-cond :patient-name
  [_ v] [:ilike "patient_name" (str "%" v "%")])

(defmethod search->where-cond :address
  [_ v] [:ilike "address" (str "%" v "%")])

(defmethod search->where-cond :policy-number
  [_ v] [:= "policy_number" v])

(defmethod search->where-cond :gender
  [_ v] [:= "gender" (v {:male "male" :female "female"})])

(defn- js-date->ts-without-tz [js-date]
  (- (.getTime js-date)
     (* (.getTimezoneOffset js-date) 60000)))

(defmethod search->where-cond :birth-date
  [_ {:keys [mode start end]}]
    (case mode
      :equal   [:=  "birth_date" (js-date->ts-without-tz start)]
      :after   [:>= "birth_date" (js-date->ts-without-tz start)]
      :before  [:<= "birth_date" (js-date->ts-without-tz start)]
      :between [:between "birth_date" (js-date->ts-without-tz start)
                                      (js-date->ts-without-tz end)]))
  
(defn- build-where-by-searches [searches]
  (->> searches
       (mapv (fn [[k v]] (search->where-cond k v)))))

(rf/reg-event-fx ::reset-searches
   (fn [cofx]
     (-> {:db (:db cofx)}
        (assoc-in [:db :searches] {})
        (assoc :fx [[:dispatch [:frontend.patients.datagrid/update-where []]]]))))

(rf/reg-event-fx ::apply-searches
  (fn [cofx]
    (let [searches (get-in cofx [:db :searches])
          where (build-where-by-searches searches)]
      (assoc {:db (:db cofx)} :fx
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
          :key name
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
                :key sub-value
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
                birth-date]} (:searches state)
        {birth-date-mode  :mode
         birth-date-start :start
         birth-date-end   :end} birth-date
        input-style {:width "100%"}]
    [:div {:id anchors/form
           :style {:padding "10px"}}

     [:p {:style {:font-weight (when (not (blank? patient-name)) "bold")}}
      (:search.atient-name locale)]

     [form-field "patient_name" :TextBox
      {:style input-style
       :value patient-name
       :onChange #(rf/dispatch [::update-searches [:patient-name] %])}]

     [:p {:style {:font-weight (when (not (blank? address)) "bold")}}
      (:search.address locale)]
     
     [form-field "address" :TextBox
      {:style input-style
       :value address
       :onChange #(rf/dispatch [::update-searches [:address] %])}] 

     [:p {:style {:font-weight (when (not (blank? policy-number)) "bold")}}
      (:search.policy-number locale)]
     
     [form-field "policy_number" :MaskedBox
      {:style input-style
       :mask "9999 9999 9999 9999"                    
       :value ((get-in Patient [:converts "policy_number" :get]) policy-number)
       :onChange #(rf/dispatch[::update-searches [:policy-number]
                            ((get-in Patient [:converts "policy_number" :set]) %)
                               ])}]

     [:p {:style {:font-weight (when (not (blank? gender)) "bold")}}
      (:search.gender locale)]

     [form-field "gender" :ButtonGroup
      {:selectionMode "single"}      
        ["any" :LinkButton {:selected (nil? gender)
                            :onClick #(rf/dispatch [::update-searches [:gender] nil])}
         (:search.gender.any locale)]

        ["male" :LinkButton {:selected (= :male gender)
                             :onClick #(rf/dispatch [::update-searches [:gender] :male])}
         (:search.gender.male locale)]

        ["female" :LinkButton {:selected (= :female gender)
                               :onClick #(rf/dispatch [::update-searches [:gender] :female])}
         (:search.gender.female locale)]]

     [:p {:style {:font-weight (when (not (blank? birth-date)) "bold")}}
      (:search.birth-date locale)]

     [form-field "birth_date_mode" :ButtonGroup
      {:selectionMode "single"}
      ["any" :LinkButton {:selected (nil? birth-date-mode)
                          :onClick #(rf/dispatch [::update-searches [:birth-date] nil])}
       (:search.birth-date.any locale)]

      ["equal" :LinkButton {:selected (= birth-date-mode :equal)
                            :onClick #(rf/dispatch [::update-searches [:birth-date :mode] :equal])}
       (:search.birth-date.equal locale)]

      ["after" :LinkButton {:selected (= birth-date-mode :after)
                           :onClick #(rf/dispatch [::update-searches [:birth-date :mode] :after])}
       (:search.birth-date.after locale)] 

       ["before" :LinkButton {:selected (= birth-date-mode :before)
                              :onClick #(rf/dispatch [::update-searches [:birth-date :mode] :before])}
        (:search.birth-date.before locale)]

       ["between" :LinkButton {:selected (= birth-date-mode :between)
                               :onClick #(rf/dispatch [::update-searches [:birth-date :mode] :between])}
        (:search.birth-date.between locale)]]

     (when (some #(= birth-date-mode %) '(:equal :after :before :between))
       [:p (when (= birth-date-mode :between) (:search.birth-date.start locale))
        [form-field "birth_date_start" :DateBox
         {:style input-style
          :value birth-date-start
          :format (:human-date-format locale)
          :calendarOptions (clj->js (:calendarOptions locale))
          :onChange #(rf/dispatch [::update-searches [:birth-date :start] %])}]])

     (when (= birth-date-mode :between)
       [:p (:search.birth-date.end locale)
        [form-field "birth_date_end" :DateBox
         {:style input-style
                     :value birth-date-end
                     :format (:human-date-format locale)
                     :calendarOptions (clj->js (:calendarOptions locale))
                     :onChange #(rf/dispatch [::update-searches [:birth-date :end] %])}]])
     [:hr]

     [ui-anchors/make-anchor anchors/reset-button
      [:> LinkButton {:onClick #(rf/dispatch [::reset-searches])} (:search.reset locale)]]
     
     [ui-anchors/make-anchor anchors/apply-button
      [:> LinkButton {:style {:float "right"}
                      :disabled (or (and (some #(= birth-date-mode %) '(:equal :after :before :between))
                                         (nil? birth-date-start))
                                    (and (= birth-date-mode :between)
                                         (nil? birth-date-end)))
                      :onClick #(rf/dispatch [::apply-searches])} (:search.apply locale)]]]))
