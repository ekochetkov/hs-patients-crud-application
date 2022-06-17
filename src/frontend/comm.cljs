(ns frontend.comm
  (:require
   [re-frame.core :as rf]
   [goog.string :as gstring]
   [websocket-fx.core :as wfx]))

(def socket-id :default)

(rf/reg-event-db :comm/error
  (fn [app-state [_ response]]
    (js/alert (str response))
    (js/console.log response)
    app-state))

(rf/reg-event-fx :comm/send-event
  (fn [cofx [_ event]]
    (let [request {:message event :on-response [:comm/receive-event]}]
      (js/console.log (str "Send event to back: "event))
      (assoc cofx :dispatch [::wfx/request socket-id request]))))

(rf/reg-event-fx :comm/receive-event
  (fn [cofx [_ event]]
   (js/console.log (str "New event dispatch from back: " event))
   (assoc cofx :dispatch event)))

(defn start [url]
  (rf/dispatch [::wfx/connect socket-id {:url url}]))
