(ns acceptance.utils
  (:require [etaoin.api :as e]
            [etaoin.keys :as k]
            [acceptance.context :refer [*driver*]]
            [clojure.string :as s]))

(defn deep-merge
  "Recursively merges maps.
   https://dnaeon.github.io/recursively-merging-maps-in-clojure/"
  [& maps]
  (letfn [(m [& xs]
            (if (some #(and (map? %) (not (record? %))) xs)
              (apply merge-with m xs)
              (last xs)))]
    (reduce m maps)))

(defn index-of [f-cond coll]
  (first (keep-indexed #(when (f-cond %2) %1) coll)))  

(defn drop-nth [n coll]
  (keep-indexed #(when (not= %1 n) %2) coll))

(defn replace-nth [n coll new-item]
  (keep-indexed #(if (= %1 n) new-item %2) coll))

(defn get-classes-set [el]
  (-> (e/get-element-attr-el *driver* el :class)
      (s/split #" ")
      set))

(defn contains-class [el class]
  (contains? (get-classes-set el) class))

(defn add-pauses [input n]
  (->> (iterate e/add-pause input)
       (take (inc n))
       last))

(defn refill-text-input-el [el raw-value]
  (e/release-actions *driver*)
  (let [value-s  (str raw-value)
        value    (if (= "" value-s)
                k/backspace
                value-s)
        mouse    (-> (e/make-mouse-input)
                  (e/add-pointer-click-el el k/mouse-left))
        keyboard (->> (str value)
                      (reduce (fn [el v]
                                (-> el
                                    (e/add-pause 40)
                                    (e/add-key-press v)
                                    (e/add-pause 40)))
                              (-> (e/make-key-input)
                                  (e/add-pause 40)
                                  (e/with-key-down k/control-left
                                    (e/add-key-press "a")))))]
      (e/perform-actions *driver* mouse)
      (e/perform-actions *driver* (e/add-pause keyboard 200)))
  (e/release-actions *driver*))

(defn refill-text-input [anchor value]
  (refill-text-input-el (e/query *driver* anchor) value))
