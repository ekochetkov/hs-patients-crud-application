(ns integration.patients-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [etaoin.api :as e]
            [etaoin.keys :as k]
            [clojure.string :as s]))

(def driver (e/chrome))

(defn classes [driver xpath]
  (-> (e/get-element-attr driver xpath :class)
      (s/split #" ")
      set))

(e/go driver "http://localhost:9009")

(e/wait-visible driver [{:id :patients-datagrid-toolbar-button-add}])

(e/get-element-text driver {:id :patients-datagrid-toolbar-button-add})

(e/click driver [{:id :patients-datagrid-toolbar-button-add}])

(e/click driver [{:id :patients-dialog_create-button-create}])

(e/click driver [{:id :patient_name}])

(e/fill driver ".//span[@id='patient_name']//input" "Ivan Ivanovich Ivanov")




(defn input-date [driver base-id year month day]
  (let [tr-index (inc (int (/ (dec month) 4)))
        td-index (inc (rem (dec month) 4))
        x-date (str ".//span[@id='" base-id "']//span[contains"
                    "(@class,'textbox-addon')]")
        x-month (str ".//div[@class='calendar-content']//table/"
                     "tbody/tr[" tr-index "]/td[contains(@class,"
                     "'calendar-nav')][" td-index "]")
        x-day (str ".//div[@class='calendar-content']//table["
                   "@class='calendar-dtable']/tbody//td[not(contains"
                   "(@class,'other-month')) and text()=" day "]")]
    (e/click driver x-date)
    (e/click driver ".//div[contains(@class,'calendar')]/span[@class='calendar-text']")
    (e/clear driver ".//input[@class='calendar-menu-year']")
    (e/fill driver ".//input[@class='calendar-menu-year']" year)
    (e/click driver x-month)
    (if (contains? (classes driver x-day) "calendar-selected")
        (e/click driver x-date)
        (e/click driver x-day))))


;(input-date driver "birth_date" 2992 1 2)

(defn input-combo [driver base-id value]
  (let [x-combo (str ".//span[@id='" base-id "']//span[contains(@class,'textbox-addon')]")
        x-item (str ".//div[contains(@class,'combo-panel')]//"
                "div[contains(@class,'combobox-item') and text()='" value "']")]
    (e/click driver x-combo)
    (e/click driver x-item)))

;(input-combo driver "gender" "Женский")
;(input-combo driver "gender" "Мужской")

(e/fill driver ".//span[@id='address']//textarea" "Some address")

(e/clear driver ".//span[@id='policy_number']//input")
                                        ;(e/fill-human driver ".//span[@id='policy_number']//input" "1111222233334444" {:mistake-prob 0.5 :pause-max 1})

(e/release-actions driver)

;(def keyboard )

;(e/refresh driver)

(def keyboard (-> (e/make-key-input)
           (e/add-key-press "1")
           (e/add-key-press "1")
           (e/add-key-press "1")
           (e/add-key-press "1")
           (e/add-key-press "2")
           (e/add-key-press "2")))

(e/perform-actions driver keyboard)

(defn add-pauses [input n]
  (->> (iterate e/add-pause input)
       (take (inc n))
       last))

(defn input-policy-number [number]
  (let [input (e/query driver ".//span[@id='policy_number']//input")
        mouse (-> (e/make-mouse-input)
                  (e/add-pointer-click-el input k/mouse-left))
        keyboard (->> number
                   (reduce (fn [input v]
                             (e/add-key-press input v))     
                           (-> (e/make-key-input)
                               (add-pauses 2)
                               (e/add-key-press k/home))))]    
   (e/perform-actions driver keyboard mouse)))

(input-policy-number "1111222233334444")
