(ns acceptance.core
  (:require [clojure.test :refer [is]]
            [clojure.string :as s]
            [etaoin.keys :as k]
            [acceptance.utils :refer [deep-merge]]))

(defn common-search-test
  "Common search test algorithm. Steps:
   1. generate noise rows
   2. generate expected rows
   3. insert all rows
   4. check actual rows equal all rows
   5. apply filter fn
   6. check actual rows equal target rows"
  [{:keys [fn-gen-fake-model-**
           fn-target-confine
           fn-insert-row
           fn-reload-datagrid
           fn-get-datagrid-data
           fn-apply-filters
           search-form
           custom-target-rows]}]

  (let [noise-rows  (fn-gen-fake-model-** 5 (complement fn-target-confine))
        target-rows (or custom-target-rows
                        (fn-gen-fake-model-** 5 fn-target-confine))
        all-rows    (concat noise-rows target-rows)]

    (doall (->> all-rows
                (map :db all-rows)
                (map fn-insert-row)))
    
    (fn-reload-datagrid)

    (let [all-actual-rows          (fn-get-datagrid-data)
          all-actual-rows-expected (map #(-> % :ru :expected) all-rows)]

    (is (= (set all-actual-rows-expected)
           (set all-actual-rows)))

    (fn-apply-filters search-form)

    (let [filtered-actual-rows          (fn-get-datagrid-data all-actual-rows)
          filtered-actual-rows-expected (map #(-> % :ru :expected) target-rows)]
        (is (= 
             (set filtered-actual-rows-expected)
             (set filtered-actual-rows)))))))

(defn ilike-parts-of-string [s]
  {:pre [(or (> (count s) 2)
             (throw (Exception. (str "Requre string length more 2. Given '" s
                                     "' Check field name in deftest."))))]}
  (let [p     (/ (count s) 4)
        lower (s/lower-case s)]
    (cond->> [(subs lower 0       (inc p))
              (subs lower (inc p) (* p 3))
              (subs lower (* p 3))]
        (not= lower (s/upper-case s))
        (mapcat #(list % (s/upper-case %))))))


(defn row-strings-not-match-strings
  [s-list new-patient-data]
  (->> s-list
       (every? (fn [s]  
                  (let [pattern (re-pattern (format "(?i).*%s.*" s))]
                    (->> (-> new-patient-data :ru :expected)
                         (every? (fn [[_ value]]
                                (not (re-matches pattern value))))))))))


(defn ilike-test
  "Common search test algorithm. Steps:
   1. init some random data model
   2. generate noise not contain parts of model testing field
   3. generate target rows
   4. denerate noise rows
   5. insert all rows
   6. check actual rows equal all rows
   7. for all ilike-strings:
      7.1. apply filter fn
      7.2. check actual rows equal target rows"
  [{:keys [fn-gen-fake-model-**
           model-field-name
           input-field-name
           fn-insert-row
           fn-reload-datagrid
           fn-get-datagrid-data
           fn-apply-filters]}]
  (let [init-target-data (first (fn-gen-fake-model-** 1))
        ilike-strings    (ilike-parts-of-string (get-in init-target-data [:ru :expected model-field-name]))
        confine-fn       (partial row-strings-not-match-strings ilike-strings)
        noise-rows       (fn-gen-fake-model-** 10 confine-fn)
        target-row       (deep-merge (-> (fn-gen-fake-model-** 1 confine-fn)
                                         first)
                                     {:db {model-field-name (get-in init-target-data [:db model-field-name])}
                                      :ru {:expected {model-field-name (get-in init-target-data [:ru :expected model-field-name])}}})
        target-rows      (list target-row)
        all-rows         (concat noise-rows target-rows)]

    (doall (->> all-rows
                (map :db all-rows)
                (map fn-insert-row)))

    (fn-reload-datagrid)

    (let [all-actual-rows          (fn-get-datagrid-data)
          all-actual-rows-expected (map #(-> % :ru :expected) all-rows)]

    (is (= (set all-actual-rows-expected)
           (set all-actual-rows)))

    (doall
     (for [ilike-str ilike-strings]
      (do
       (fn-apply-filters {input-field-name ilike-str})

       (let [filtered-actual-rows          (fn-get-datagrid-data all-actual-rows)
             filtered-actual-rows-expected (map #(-> % :ru :expected) target-rows)]
        (is (= 
             (set filtered-actual-rows-expected)
             (set filtered-actual-rows)))

        (fn-apply-filters {input-field-name k/backspace})
        (fn-get-datagrid-data filtered-actual-rows))))))))
