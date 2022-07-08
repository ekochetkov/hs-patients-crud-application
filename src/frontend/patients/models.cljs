(ns frontend.patients.models
  (:require [clojure.string :refer [trim replace blank? join]]))

(def locales {:en {:human-date-format "yyyy-MM-dd"}
              :ru {:human-date-format "dd.MM.yyyy"}})

(def locale (:en locales))

(def Patient
  {"patient_name" {:label "Patient name"
                   :set-fn trim
                   :rc-input-class :TextBox}

   "birth_date" {:label "Birth date"
                 :set-fn #(.getTime %)
                 :get-fn #(js/Date. %)
                 :rc-input-class :DateBox    
                 :rc-input-attrs {:format (:human-date-format locale)}}
   
   "gender" {:label "Gender"
             :rc-input-class :ComboBox    
             :rc-input-attrs {:data [{:value "male" :text "Male"}
                                     {:value "female" :text "Female"}]}}
   
   "address" {:label "Address"
              :rc-input-class :TextBox
              :set-fn trim
              :rc-input-attrs {:multiline true :style {:height "70px"}}}

   "policy_number" {:label "Policy_number"
                    :set-fn #(-> %
                                 trim
                                 (replace " " "")
                                 (replace "_" ""))
                    :get-fn (fn [v]
                              (let [padd (repeat (- 16 (count v)) \_)
                                    v-with-padd (str v (join padd))]
                              (str (->> (partition-all 4 v-with-padd)
                                              (map #(join %))
                                              (join " ")))))
                    :rc-input-class :MaskedBox
                    :rc-input-attrs {:mask "9999 9999 9999 9999"}}})
