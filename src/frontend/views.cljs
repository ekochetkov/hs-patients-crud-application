(ns frontend.views
  (:require [re-frame.core :as rf]))

(rf/reg-sub :show-disconnect-error #(:show-disconnect-error %))

(defn lost-connection-to-server-notif []
  "Notif user about problem with connection to server"
  (let [show (rf/subscribe [:connection-error-conseq])]
    (when @show
      [:div.alert.danger {:role "alert"}
        "Connection to server lost. Try reconnection..."])))
