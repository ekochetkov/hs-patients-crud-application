(ns frontend.utils)

(defn js-date->ts-without-tz [js-date]
  (- (.getTime js-date)
     (* (.getTimezoneOffset js-date) 60000)))

(defn with-id [id content]
  [:span {:id id} content])
