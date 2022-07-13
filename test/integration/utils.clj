(ns integration.utils
  (:require [etaoin.api :as e]
            [clojure.string :as s]))
 
(defn classes [driver xpath]
  (-> (e/get-element-attr driver xpath :class)
      (s/split #" ")
      set))

(defn add-pauses [input n]
  (->> (iterate e/add-pause input)
       (take (inc n))
       last))
