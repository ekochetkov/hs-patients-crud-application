(ns acceptance.easy-ui.layout-panel
  (:require [etaoin.api :as e]
            [acceptance.utils :as u]
            [acceptance.context :refer [*driver*]]))

;; query

(defn query [anchor]
  (e/query *driver*
           (format ".//span[@id='%s']/ancestor::div[2]"
                   anchor)))

;; opened?/collapsed?

(defn collapsed? [anchor]
  (u/contains-class
     (query anchor)
     "layout-collapsed"))

(def opened? (complement collapsed?))

