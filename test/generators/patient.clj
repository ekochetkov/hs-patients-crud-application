(ns generators.patient
  (:import (java.time ZonedDateTime ZoneId))
  (:require [clojure.string :as s]))

(defn birth_date []
  (let [y (rand-nth (range 1980 2011))
        m (rand-nth (range 1 13))
        d (rand-nth (range 1 28))        
        milli (-> (ZonedDateTime/of y m d 0 0 0 0 (ZoneId/of "GMT"))
                  (.toInstant)
                  (.toEpochMilli))
        add-zero #(if (< % 10) (str "0" %) %)]
    {:db milli
     :input milli
     :expected {:ru (format "%s.%s.%s" (add-zero d) (add-zero m) y)
                :en (format "%s/%s/%s" (add-zero m) (add-zero d) y)}}))

(defn gender []
  (rand-nth [{:db "male"
              :input    {:ru "Мужской" :en "Male"}
              :expected {:ru "Мужской" :en "Male"}}
             {:db "female"
              :input    {:ru "Женский" :en "Female"}
              :expected {:ru "Женский" :en "Female"}}]))

(defn policy_number []
  (let [digits (repeatedly 16 #(inc (rand-int 9)))
        digits-plain (apply str digits)
        digits-grouped (->> digits
                            (partition 4)
                            (map #(apply str %))
                            (s/join " " ))]
    {:db digits-plain
     :input digits-plain
     :expected digits-grouped}))

(defn chr-range [seq-of-pairs]
  (map char (->> seq-of-pairs
                (mapcat (fn [[a b]] (range (int a) (int b)))))))

(defn word-gen [{:keys [word-size capitalize]}]
  (let [chrs (chr-range '([\0 \9] [\a \z] [\а \я]))
        word-len (-> (apply range word-size)
                     (rand-nth))]
    (-> (apply str (repeatedly word-len #(rand-nth chrs)))
        (cond->
            capitalize (s/capitalize)))))

(defn sentence-gen [{:keys [sentence-word-count] :as arg}]
  (let [word-count (-> (apply range sentence-word-count)
                       (rand-nth ))]
    (->> (repeatedly word-count #(word-gen arg))
         (s/join " "))))

(defn patient_name []
  (let [name (sentence-gen {:sentence-word-count [2 4]
                            :word-size [4 9]
                            :capitalize true})]
    {:all name}))

(defn address []
  (let [address (sentence-gen {:sentence-word-count [4 8]
                               :word-size [4 9]
                               :capitalize true})]
    {:all address}))

(defn kws [data kw]
  (reduce (fn [acc [k v]] (assoc acc k (or (:all v) (kw v) v))) {}  data))

(defn group [data]
  (reduce conj
    {:db (kws data :db)}
    (->> '(:ru :en) (map (fn [lang] {lang (reduce conj
          (->> '(:input :expected) (map #(hash-map % (kws (kws data %) lang)))))})))))

(defn raw-random-variant []
  (group {"patient_name" (patient_name)
          "gender" (gender)
          "birth_date" (birth_date)
          "address" (address)
          "policy_number" (policy_number)}))

(def iterations-max-count 10000)

(defn gen-fake-patient
  ([] (gen-fake-patient 0 nil))
  ([ confine ] (gen-fake-patient 0 confine))
  ([ iterations-counter confine ]   
   (let [new-variant (raw-random-variant)]
    (when (> iterations-counter
             iterations-max-count)
      (throw (Exception. (format (str "After %s iterations there is no suitable "
                                      "combination of random data. "
                                      "Check feasibility constraints.")
                                 iterations-max-count))))
    (if (or (nil? confine)
            (confine new-variant))
      new-variant
      (recur (inc iterations-counter) confine)))))
  
(defn gen-unique-fake-patients
  ([n] (gen-unique-fake-patients n nil))
  ([n confine] (gen-unique-fake-patients n confine nil))
  ([n confine acc]
   (if (zero? n)
     acc
     (let [new-patient (gen-fake-patient confine)]
       (if (some #(= % new-patient) acc)
         (recur n confine acc)
         (recur (dec n) confine (conj acc new-patient)))))))


;(gen-unique-fake-patients 1)
