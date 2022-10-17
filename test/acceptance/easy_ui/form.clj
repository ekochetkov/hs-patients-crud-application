(ns acceptance.easy-ui.form
  (:require
   [acceptance.utils :as u]
   [acceptance.context :refer [*driver*]]
   [acceptance.easy-ui.link-button :as link-button]
   [etaoin.api :as e])
  (:import (java.util Calendar GregorianCalendar Date)))

(defn wait-first-field [id]
  (e/wait-visible *driver*
      (format ".//div[@id='%s']//span[@type='field']" id)))

;; Get values from fields

(defmulti get-field-value
  (fn [field-type _]
    (case field-type
      ("TextBox" "ComboBox" "DateBox" "MaskedBox" "SearchBox") :as-input
      "TextArea" :as-text-area
      "ButtonGroup" :ButtonGroup)))

(defmethod get-field-value :as-input [_ el]
  (let [input-el (e/child *driver* el {:tag :input})]
    (e/get-element-attr-el *driver* input-el :value)))

(defmethod get-field-value :as-text-area [_ el]
  (let [textarea-el (e/child *driver* el {:tag :textarea})]
    (e/get-element-text-el *driver* textarea-el)))

(defmethod get-field-value :ButtonGroup [_ el]
  (let [buttons (e/children *driver* el {:type "subfield"})]
    (->> buttons
         (filter #(link-button/selected-el? (link-button/query-el %)))
         first
         (#(e/get-element-attr-el *driver* % :value)))))

(defn get-values [anchor]
  (let [parent (e/query *driver* {:id anchor})
        fields (e/children *driver* parent {:tag :span :type :field})]
    (->> fields
         (reduce (fn [acc el]
                  (e/wait-predicate #(e/displayed-el? *driver* el))
                  (let [field-type (e/get-element-attr-el *driver* el "fieldtype")]
                     (assoc acc
                            (e/get-element-attr-el *driver* el "id")
                            (get-field-value field-type el)))) {}))))

;; Set values to fields

(defmulti set-field-value
  (fn [field-type _ _] field-type))

(defmethod set-field-value "TextBox" [_ el value]
  (let [input-el (e/child *driver* el {:tag :input})]
    (u/refill-text-input-el input-el value)))

(defmethod set-field-value "SearchBox" [_ el value]
  (let [input-el (e/child *driver* el {:tag :input})]
    (u/refill-text-input-el input-el value)))
    
(defmethod set-field-value "TextArea" [_ el value]
  (let [input-el (e/child *driver* el {:tag :textarea})]
    (u/refill-text-input-el input-el value)))
    
(defmethod set-field-value "ComboBox" [_ el value]
  (let [x-combo (e/child *driver* el 
                     ".//span[contains(@class,'textbox-addon')]")
        x-item (str "."
                    "//div[contains(@class,'combo-panel')]"
                    "//div[contains(@class,'combobox-item') and text()='" value "']")]
    (e/wait-predicate #(e/displayed-el? *driver* el))    
    (e/click-el *driver* x-combo)
    (e/click *driver* x-item)))

(defmethod set-field-value "MaskedBox" [_ el value]
  (let [input-el (e/child *driver* el {:tag :input})]  
    (e/wait-predicate #(e/displayed-el? *driver* el))
    (u/refill-text-input-el input-el value)))

(defn ts->year-month-day [ts]
  (->> (doto
       (GregorianCalendar/getInstance)
       (.setTime (Date. ts)))
     ((juxt #(.get % Calendar/YEAR)
            #(inc (.get % Calendar/MONTH))
            #(.get % Calendar/DAY_OF_MONTH)))))

(defmethod set-field-value "ButtonGroup" [_ el value]
  (let [buttons (e/children *driver* el {:type "subfield"})]
     (e/wait-predicate #(e/displayed-el? *driver* el))
     (->> buttons
          (filter #(= (e/get-element-attr-el *driver* % :value)
                      value))
          first
          (e/click-el *driver*))))

(defmethod set-field-value "DateBox" [_ el value]
  (let [[year month day] (ts->year-month-day value)
        tr-index (inc (int (/ (dec month) 4)))
        td-index (inc (rem (dec month) 4))
        x-date (e/child *driver* el
                    ".//span[contains(@class,'textbox-addon')]")
        x-month (str "."
                     "//div[@class='calendar-content']"
                     "//table//tr[" tr-index "]"
                     "/td[contains(@class,'calendar-nav')][" td-index "]")
        x-day (str "."
                   "//div[@class='calendar-content']"
                   "//table[@class='calendar-dtable']"
                   "//td[not(contains(@class,'other-month')) and text()=" day "]")]

    (e/wait-predicate #(e/displayed-el? *driver* el))
    (e/click-el *driver* x-date)
    
    (e/wait-visible *driver* ".//div[contains(@class,'calendar')]/span[@class='calendar-text']")
    (e/click *driver* ".//div[contains(@class,'calendar')]/span[@class='calendar-text']")
  
    (e/wait-visible *driver* ".//input[@class='calendar-menu-year']")    
    (u/refill-text-input ".//input[@class='calendar-menu-year']" year)
  
    (e/wait-visible *driver* x-month)

    (e/click *driver* x-month)
  
    (e/wait-visible *driver* x-day)
    (if (u/contains-class (e/query *driver* x-day) "calendar-selected")
        (e/click-el *driver* x-date)
        (e/click *driver* x-day))))

(defn find-target-field [form-el field-name]
    (->> (e/children *driver* form-el {:tag :span :type :field})
         (filter (fn [el]
                   (e/wait-predicate #(e/displayed-el? *driver* el))
                   (= (e/get-element-attr-el *driver* el "id")
                      field-name)))
         first))

(defn set-values [id data]
  (e/wait-visible *driver* {:id id})
  (let [form-el (e/query *driver* {:id id})]
  (doall (map (fn [[name value]]
                (let [el (find-target-field form-el name)]
                  (e/wait-predicate #(e/displayed-el? *driver* el))
                  (let [field-type (e/get-element-attr-el *driver* el "fieldtype")]
                    (set-field-value field-type el value)))) data))))

