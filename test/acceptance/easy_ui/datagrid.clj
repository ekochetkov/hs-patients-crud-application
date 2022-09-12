(ns acceptance.easy-ui.datagrid
  (:require [etaoin.api :as e]
            [clojure.string :as s]
            [etaoin.keys :as k]
            [acceptance.utils :as u]
            [acceptance.context :refer [*driver*]]))

(defn datagrid-data [anchor]
  (let [header  (-> (e/get-element-attr *driver* {:id anchor} :fields)
                    (s/split #","))
        rows-el (e/children *driver* (e/query *driver* {:id anchor})
                            {:tag "tbody/tr[contains(@class,'datagrid-row')]"})
        rows    (->> rows-el
                     (map (fn [row] (->> (e/children *driver* row ".//td/div")
                                         (map

                                        ;#(e/get-element-text-el *driver* %)
                                          #(e/get-element-property-el *driver* % :textContent )                                     

                                          )))))]
    (mapv #(zipmap header %) rows)))

(defn wait-datagrid-data-changed
  "Wait until datagrid rows changed and return its without '#' column"
  ([dg-anchor]
     (wait-datagrid-data-changed dg-anchor nil))
  ([dg-anchor current-data]
   (e/wait-invisible *driver* ".//div[@class='datagrid-mask']")
   (let [check-interval-sec 0.25
         total-attempts 40]
     (loop [last-attempts total-attempts]
       (when (zero? last-attempts)
         (throw (Exception. (format "Datagrid data not changed after %.2f seconds"
                                    (* total-attempts check-interval-sec)))))
       (let [new-data (->> (datagrid-data dg-anchor)
                           (map #(dissoc % "#")))]
         (if (= new-data current-data)
           (do (e/wait check-interval-sec)
               (recur (dec last-attempts)))
           new-data))))))

(defn context-menu [anchor index item-selector]
  (let [datagrid-el (e/query *driver* {:id anchor})
        rows-selector {:tag "tbody/tr[@class='datagrid-row']"}
        rows-els (e/children *driver* datagrid-el rows-selector)
        row-el (get rows-els index)
        mouse (-> (e/make-mouse-input)
                  (e/add-pointer-click-el row-el k/mouse-right))]
    (e/release-actions *driver*)
    (e/perform-actions *driver* mouse)
    (e/release-actions *driver*))

  (let [menu-item-update-selector (str ".//span[@class='menu-inline']" item-selector)]
    (e/wait-visible *driver* menu-item-update-selector)
    (e/click *driver* menu-item-update-selector)))

(defn query [anchor]
  (e/query *driver* {:id anchor}))



(defn click-next-page [anchor]
  (e/click-el *driver*
              (e/child *driver* (query anchor)
                       {:class "l-btn-icon pagination-next"})))

(defn pagination-info [anchor]
  (let [parent (query anchor)
        child (e/child *driver* parent {:class "pagination-info"})]
    (e/get-element-text-el *driver* child)))

(defn next-page-not-exists? [anchor]
  (let [datagrid (query anchor)
        pagination-row (e/child *driver* datagrid ".//div[contains(@class,'pagination')]")
        next-button (e/child *driver* pagination-row ".//span[@class='l-btn-icon pagination-next']/ancestor::a[1]")]
    (u/contains-class next-button "l-btn-disabled")))

(def next-page-exists? (complement next-page-not-exists?))

(defn get-data-by-pages [anchor]
  (loop [page-counter 0 acc {} previos-items nil]
    (let [current-items (wait-datagrid-data-changed anchor previos-items)
          new-acc       (assoc acc
                               page-counter {:paginator-info (pagination-info anchor)
                                             :items          current-items})]
      (if (next-page-exists? anchor)
        (do
          (click-next-page anchor)
          (recur (inc page-counter)
                 new-acc
                 current-items))
       new-acc))))
