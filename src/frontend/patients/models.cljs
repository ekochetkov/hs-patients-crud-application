(ns frontend.patients.models
  (:require [clojure.string :refer [trim replace blank? join]]
            [frontend.utils :refer [js-date->ts-without-tz]]))

(def locales {:en {:human-date-format "yyyy-MM-dd"}
              :ru {:human-date-format "dd.MM.yyyy"}})

(def locale (:en locales))

(def Patient
  {:converts {"patient_name" {:set trim}
              "address" {:set trim}             
              "birth_date" {:set js-date->ts-without-tz
                            :get #(js/Date. %)}             
              "policy_number" {:set #(-> %
                                trim
                                (replace " " "")
                                (replace "_" ""))
                               :get (fn [v]
                                         (let [padd (repeat (- 16 (count v)) \_)
                                               v-with-padd (str v (join padd))]
                                            (str (->> (partition-all 4 v-with-padd)
                                               (map #(join %))
                                               (join " ")))))}}

   
   :fields
  {:patient-name {:name "patient_name"
                  :rc-input-class :TextBox
                  :rc-input-attrs (fn [_] {})}

   :birth-date {:name "birth_date"

                :rc-input-class :DateBox    
                :rc-input-attrs (fn [locale]
                                  {:format (:human-date-format locale)})}

;                                   (:human-date-format locale)})}
   
   :gender {:name "gender"
            :rc-input-class :ComboBox    
            :rc-input-attrs (fn [locale]
                               {:data [{:value "male" :text (:gender.male locale)}
                                       {:value "female" :text (:gender.female locale)}]})}
   
   :address {:name "address"
             :rc-input-class :TextBox
             :rc-input-attrs (fn [locale]
                                {:multiline true :style {:height "70px"}})}

   :policy-number {:name "policy_number"
                   :rc-input-class :MaskedBox
                   :rc-input-attrs (fn [locale]
                                          {:mask "9999 9999 9999 9999"})}}})
