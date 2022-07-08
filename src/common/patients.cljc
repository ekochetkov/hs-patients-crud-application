(ns common.patients)

(def validation-rules
  {"patient_name" {"required" true
                   "rule" {"validator" #(re-matches #"^[\s\dA-zА-я]{5,}$" %)
                           "message" "The name may contains only characters and numbers legth more 5"}}
   "policy_number" {"required" true
                    "rule" {"validator" #(re-matches #"^[\d]{16}$" %)
                            "message" "The policy number contains 16 digits"}}
   "birth_date" {"required" true}
   "gender" {"required" true}
   "address" {"required" true}})
   