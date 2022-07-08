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

(rf/reg-event-fx ::send-event
  (fn [cofx [_ event-handler-id event]]
    (let [request {:message event :on-response [:comm/receive-event event-handler-id]}]
      (js/console.log (str "Send event to back: " event  ))
      (assoc cofx :fx [[:dispatch [::wfx/request socket-id request]]]))))

(rf/reg-event-fx :comm/receive-event
  (fn [cofx [_ event-handler-id event-from-back]]
   (js/console.log (str "New event dispatch from back: " event-from-back " handled by " event-handler-id))
   (assoc cofx :fx [[:dispatch [event-handler-id event-from-back]]])))

(defn start [url]
  (rf/dispatch [::wfx/connect socket-id {:url url}]))  
