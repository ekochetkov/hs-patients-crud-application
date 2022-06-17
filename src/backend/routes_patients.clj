(ns backend.routes-patients
  (:require [schema.core :as s]))

(defn patients [where]
  (let [schema {}]
    
      )
  )

(def patients-resource-schema
  {
   (s/required-key :patient-name) s/Str
   (s/required-key :gender) s/Str
   (s/required-key :birth-day) s/Str
   (s/required-key :address) s/Str
   (s/required-key :policy_number) s/Str    
   })

(def patients-update-schema
  {
   (s/required-key :uuid) s/Str
   (s/required-key :resource) patients-resource-schema
   })
