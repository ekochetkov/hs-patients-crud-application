(ns common.patients
  (:require [schema.core :as s]
            #_[schema.utils :as u]))

(def user-resource-schema
  {"patient_name" {:pred #(re-matches #"^[\s\d\w]{5,}$" %)
                   :message "The name may contains only characters and numbers legth more 5"}
   "policy_number" {:pred #(re-matches #"^[\d]{16}$" %)
                    :message "The policy number contains 16 digits"}})

(def db-resource-schema
  {(s/required-key "patient_name") (s/conditional
          (get-in user-resource-schema ["patient_name" :pred]) s/Str)
   (s/required-key "policy_number") (s/conditional
          (get-in user-resource-schema ["patient_name" :pred]) s/Str)   
   (s/required-key "birth_date") s/Int
   (s/required-key "gender") (s/enum "male" "female")
   (s/required-key "address") s/Str})

(def db-row-schema
  {(s/required-key :uuid) java.util.UUID
   (s/required-key :deleted) (s/maybe s/Int)
   (s/required-key :resource) db-resource-schema})
