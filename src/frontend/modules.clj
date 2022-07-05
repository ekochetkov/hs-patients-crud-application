(ns frontend.modules
  (:require
   [clojure.string :refer [replace]]
   [clojure.data :refer [diff]]
   [re-frame.core]))

(defn ns->module-name [ns] 
  (keyword (replace ns "frontend" "module")))

(defmacro isolate-module-state [module-name event-name func]
  `(fn [app-state# event#]
     (let [module-state-current# (~module-name app-state#)
           module-state-new# (~func module-state-current# event#)
           diffs# (diff module-state-current# module-state-new#)]
       (js/console.log
          "Update state in" (str ~module-name) "on event" (str ~event-name)
          "chandged" (str (second diffs#)))
       (assoc app-state# ~module-name module-state-new#))))

(defmacro reg-event-db [event-name func]
  `(let [module-name# ~(ns->module-name *ns*)
         full-event-name# (keyword module-name# ~event-name)]
     (js/console.log "reg event name" (str full-event-name#))
      (re-frame.core/reg-event-db full-event-name#
         (partial (isolate-module-state module-name# ~event-name ~func)))))

(defmacro dispatch [event]
  `(let [module-name# ~(ns->module-name *ns*)
         event-name# (get ~event 0)
         full-event-name# (keyword module-name# event-name#)]
;     (js/console.log "dispatch" (str full-event-name#))
     (re-frame.core/dispatch (assoc ~event 0 full-event-name#))))

(defmacro reg-sub [key func]
  `(let [module-name# ~(ns->module-name *ns*)
         full-key-name# (keyword module-name# ~key)]
     (re-frame.core/reg-sub full-key-name#
        (fn [app-state#] (~func (module-name# app-state#))))))

(defmacro subscribe [[key]]
  `(let [module-name# ~(ns->module-name *ns*)
         full-key-name# (keyword module-name# ~key)]
     (re-frame.core/subscribe [full-key-name#])))
