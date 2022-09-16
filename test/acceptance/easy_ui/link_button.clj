(ns acceptance.easy-ui.link-button
  (:require [etaoin.api :as e]
            [acceptance.utils :as u]
            [acceptance.context :refer [*driver*]]))

;; query

(defn query-el [el]
  (e/child *driver* el {:tag :a}))

(defn query [anchor]
  (query-el (e/query *driver* {:id anchor})))

;; disabled?/enabled?

(defn disabled-el? [el]
  (u/contains-class el "l-btn-disabled"))

(def enabled-el?
  (complement disabled-el?))

(defn disabled? [anchor]
  (disabled-el? (query anchor)))

(def enabled?
  (complement disabled?))

;; click

(defn click-el [el]
  (e/wait-predicate #(enabled-el? el))
  (e/click-el *driver* el))

(defn click [anchor]
  (e/wait-exists *driver* {:id anchor})
  (click-el (query anchor)))

;; selected?/unselected?

(defn selected-el? [el]
  (u/contains-class el "l-btn-selected"))

(def unselected-el?
  (complement selected-el?))

(defn selected? [anchor]
  (selected-el? (query anchor)))

(def unselected?
  (complement selected?))
