(ns common.patients)

(def validation-rules
  {"patient_name" {"required" true
                   "rule" {"validator" #(re-matches #"^[\s\dA-zА-я]{5,}$" %)
                           "message" :patient_name}}
   "policy_number" {"required" true
                    "rule" {"validator" #(re-matches #"^[\d]{16}$" %)
                            "message" :policy_number}}
   "birth_date" {"required" true}
   "gender" {"required" true}
   "address" {"required" true}})
   
