(ns backend.utils
  (:require
   [backend.context :refer [ctx]]
   [backend.ws :refer [ws-process-event]]
   [backend.patients-events]   
   [backend.db]
   [clojure.data.json :as json]
   [joda-time :as jt]
   [clojure.string :refer [join]]
   [org.httpkit.client :as http]))

(defn seed-patients-table [count]
  (let [url "https://api.randomdatatools.ru/"
        opts {:query-params {"count" count
                             "unescaped" "false"
                             "params" (join "," ["LastName" "FirstName" "FatherName"
                                                 "Gender" "DateOfBirth" "Address" "oms"])}}
        date->epoch (fn [^String date]
                      (jt/to-millis-from-epoch
                       (jt/parse-date-time
                        (jt/formatter "dd.MM.yyyy" ) date)))
        gender-map {"Женщина" "female"
                    "Мужчина" "male"}]
    @(http/get url opts
       (fn [{:keys [body error]}]
            (if error
              (println "Failed, exception is " error)
              (let [rows (json/read-str body)]
                (doall (for [row rows]
                  (let [resource {"patient_name" (str (get row "LastName") " "
                                                      (get row "FirstName") " "
                                                      (get row "FatherName"))
                                  "gender" (get gender-map (get row "Gender"))
                                  "address" (get row "Address")
                                  "policy_number" (str (get row "oms"))
                                  "birth_date" (date->epoch (get row "DateOfBirth"))}]
                    (ws-process-event ctx :patients/create [resource]))))))))))


