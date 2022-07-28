(ns integration.utils
  (:require [etaoin.api :as e]
            [etaoin.keys :as k]
            [clojure.string :as s]))
 
(defn classes [driver xpath]
  (-> (e/get-element-attr driver xpath :class)
      (s/split #" ")
      set))

(defn add-pauses [input n]
  (->> (iterate e/add-pause input)
       (take (inc n))
       last))

(defn refill-text-input [driver xpath value]
    (let [input (e/query driver xpath)
          mouse (-> (e/make-mouse-input)
                    (e/add-pointer-click-el input k/mouse-left))
             keyboard (->> (str value)
                   (reduce (fn [input v]
                             (e/add-key-press input v))     
                           (-> (e/make-key-input)
                               (add-pauses 2)
                               (e/with-key-down k/control-left
                                 (e/add-key-press "a"))
                               )))]
  
  (e/perform-actions driver keyboard mouse))
  (e/release-actions driver))
