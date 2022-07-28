(ns integration.easy-ui
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [integration.utils :refer [classes add-pauses refill-text-input]]
            [etaoin.api :as e]
            [etaoin.keys :as k]
            [clojure.string :as s]))

(defn get-input [driver base-id]
  (e/get-element-attr driver (str ".//span[@id='" base-id "']//input") :value))

(defn get-textarea [driver base-id]
  (e/get-element-text driver (str ".//span[@id='" base-id "']//textarea")))

(defn fill-input [driver base-id value]
  (e/fill driver (str ".//span[@id='" base-id "']//input") value))

(defn fill-textarea [driver base-id value]
  (e/fill driver (str ".//span[@id='" base-id "']//textarea") value))

(defn fill-date [driver base-id year month day]
  (let [tr-index (inc (int (/ (dec month) 4)))
        td-index (inc (rem (dec month) 4))
        x-date (str "."
                    "//span[@id='" base-id "']"
                    "//span[contains(@class,'textbox-addon')]")
        x-month (str "."
                     "//div[@class='calendar-content']"
                     "//table//tr[" tr-index "]"
                     "/td[contains(@class,'calendar-nav')][" td-index "]")
        x-day (str "."
                   "//div[@class='calendar-content']"
                   "//table[@class='calendar-dtable']"
                   "//td[not(contains(@class,'other-month')) and text()=" day "]")]
    (e/wait 1)
    (e/click driver x-date)
    (e/wait 1)    
    (e/click driver ".//div[contains(@class,'calendar')]/span[@class='calendar-text']")
    (e/wait 1)    
    (refill-text-input driver ".//input[@class='calendar-menu-year']" year)
    (e/wait 1)    
    (e/click driver x-month)
    (e/wait 1)
    (if (contains? (classes driver x-day) "calendar-selected")
        (e/click driver x-date)
        (e/click driver x-day))))

(defn fill-combo [driver base-id value]
  (let [x-combo (str "."
                     "//span[@id='" base-id "']"
                     "//span[contains(@class,'textbox-addon')]")
        x-item (str "."
                    "//div[contains(@class,'combo-panel')]"
                    "//div[contains(@class,'combobox-item') and text()='" value "']")]
    (e/click driver x-combo)
    (e/click driver x-item)))

(defn fill-masked [driver base-id number]
  (e/release-actions driver)  
  (let [input (e/query driver (str ".//span[@id='" base-id "']//input"))
        mouse (-> (e/make-mouse-input)
                  (e/add-pointer-click-el input k/mouse-left))
        keyboard (->> number
                   (reduce (fn [input v]
                             (e/add-key-press input v))     
                           (-> (e/make-key-input)
                               (add-pauses 2)
                               (e/add-key-press k/home))))]
  (e/perform-actions driver keyboard mouse))
  (e/release-actions driver))

