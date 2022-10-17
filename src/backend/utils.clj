(ns backend.utils
  (:require
   [backend.context :refer [ctx]]
   [backend.ws :refer [ws-process-event]]
   [backend.patients-events]   
   [backend.db]
   [clojure.data.json :as json]
   [clojure.string :refer [join split]]
   [org.httpkit.client :as http])
  (:import (java.time ZonedDateTime ZoneId)))


(defn get-real-patient-random-data [count]
  (let [url  "https://api.randomdatatools.ru/"
        opts {:query-params {"count"     count
                             "unescaped" "false"
                             "params"    (join "," ["LastName" "FirstName" "FatherName"
                                                    "Gender" "DateOfBirth" "Address" "oms"])}}]
    @(http/get url
               opts
               (fn [{:keys [body error]}]
                 (if error
                   (println "Failed, exception is " error)
                   (json/read-str body))))))

(defn date->epoch [date-str]
  (let [date-str-parts (split date-str #"\.")
        y              (Integer/parseInt (get date-str-parts 2))
        m              (Integer/parseInt (get date-str-parts 1))
        d              (Integer/parseInt (get date-str-parts 0))]
                         (-> (ZonedDateTime/of y m d 0 0 0 0 (ZoneId/of "GMT"))
                             (.toInstant)
                             (.toEpochMilli))))

(def gender-map {"Женщина" "female"
                 "Мужчина" "male"})

(defn insert-patient-data [row]
  (let [resource {"patient_name"  (str (get row "LastName") " "
                                       (get row "FirstName") " "
                                       (get row "FatherName"))
                  "gender"        (get gender-map (get row "Gender"))
                  "address"       (get row "Address")
                  "policy_number" (str (get row "oms"))
                  "birth_date"    (date->epoch (get row "DateOfBirth"))}]
    (ws-process-event ctx [:patients/create resource])))

(defn seed-patients-table [count]
  (doall
   (->> (get-real-patient-random-data count)
        (map insert-patient-data))))
  

