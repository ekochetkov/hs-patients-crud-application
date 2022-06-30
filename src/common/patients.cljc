(ns common.patients
  (:require [schema.core :as s]))

  #_(let [resource {"gender" "male" 
                  "address" "New Zealand, Taranaki, Taupo, Bucs Road st. 2296"  s/Str
                  "birth_date" 702604800 s/Int
                  "patient_name" "Brad Morris" s/Str
                    "policy_number" "5492505115922541" s/Str
                    }])

(def error-messages
  {"patient_name"
   (fn [error]
     (let [sch (.schema error)]
       
     )
   "The name may contains only characters and numbers legth more 5"
   "gender" "The field 'gender' may contains only 'male', 'famale' values"})

(def resource-schema
  {(s/required-key "patient_name")
     (s/conditional #(> (count %) 5) s/Str)
   (s/required-key "birth_date") s/Int
   (s/required-key "gender") (s/enum "male" "female")
   (s/required-key "address") s/Str
   (s/required-key "policy_number") s/Str})

(def db-row-schema
  {(s/required-key :uuid) java.util.UUID
   (s/required-key :deleted) s/Int
   (s/required-key :resource) resource-schema})

(defn validation-error-details [error]
  (let [values (juxt #(.schema %) 
                     #(.value %)
                     #(.expectation-delay %)
                     #(.fail-explanation %))]
    (->> error :error values)))

(defn resource-schema-error-decoder [errors]
  (map (fn [[field error]]
;         (validation-error-details error)
;         error
;         (= error (symbol 'missing-required-key))
;         (type error)
;       (type (symbol 'missign-required-key))
         (or  (cond 
                 (= (symbol 'missing-required-key) error)
                    {field {:human "This field is required"
                            :code :missign-required-key}}
                                        ;false true
                    (= schema.utils.ValidationError (type error))
                      {field {:human (get error-messages field)
                              :code :validation-error
                              :de [(.schema error)
                                   (.value error)
                                   (.expectation-delay error)
                                   (.fail-explanation error)]
                              :error error}}                    
                    )
              {field [error (type error)]}
                                        ;              error
              )
         ) errors))

(try
  (s/validate resource-schema {"patient_name" 23 "gender" "male" "address" "df"})
  (catch Exception e
     (-> e .getData :error resource-schema-error-decoder  #_resource-schema-error-decoder )))

;         {field {:human "Unknow validation data error"
;                 :code :unknow-validation-error}}


;(try
;(s/validate db-row-schema {:resource {}})
;  (catch Exception e
;                                             (-> e .getData :error  )))


