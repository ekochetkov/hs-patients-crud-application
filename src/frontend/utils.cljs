(ns frontend.utils
  (:require ["rc-easyui" :refer [dateHelper]]))

(defn js-date->ts-without-tz [js-date]
  (- (.getTime js-date)
     (* (.getTimezoneOffset js-date) 60000)))

(defn with-id [id content]
  [:span {:id id} content])

(defn ts->human-date [ts format]
  (let [date (new js/Date ts)]
     (.formatDate dateHelper date format)))
