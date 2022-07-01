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


#_(defn resource-schema-error-decoder [errors]
  (map (fn [[field error]]
         (or  (cond 
                 (= (symbol 'missing-required-key) error)
                    {field {:human "This field is required"
                            :code :missign-required-key}}
                    (= schema.utils.ValidationError (type error))
                    (let [schema-error (.schema error)]
                      (case schema-error
                        java.lang.String {field {:h "Not valid data type" :c "Expected string type"}}


                        {field {:p (u/validation-error-explain error)
                                :z @(.expectation-delay error)
                                :fail (.fail-explanation error)
                                :t (type error)
                                :r schema-error
                                :x (type schema-error)
                                := (= schema-error java.lang.String)}}
                            )

                      
                        #_{field {:human "h" ;(get error-messages field)
                              :code :validation-error
                              :de [(.schema error)
                                   (.value error)
                                   (.expectation-delay error)
                                   (.fail-explanation error)]
                              :error error}}                    )
                    )
              {field [error (type error)]}
                                        ;              error
              )
         ) errors))

#_(try
  (s/validate resource-schema {"res" 2
                                        "pn" false "uuid" "sdf" "policy_number" 33 "patient_name" false "gender" 123 "birth_date" "d"  "address" 34
                               })
  (catch Exception e
     (-> e .getData :error resource-schema-error-decoder  #_resource-schema-error-decoder )))

;         {field {:human "Unknow validation data error"
;                 :code :unknow-validation-error}}


;(try
;(s/validate db-row-schema {:resource {}})
;  (catch Exception e
;                                             (-> e .getData :error  )))


