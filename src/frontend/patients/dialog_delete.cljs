(ns frontend.patients.dialog-delete)

                                        ;(def
(rf/reg-event-fx :patients/send-event-delete
  (fn [cofx _]
    (let [uuid (aget (get-in cofx [:db :patients :selection]) "uuid")]
        (assoc cofx :dispatch [:comm/send-event [:patients/delete uuid]]))))


(rf/reg-event-fx :patients/delete
  (fn [cofx _]
    (-> cofx
      (assoc-in [:db :patients :show-dialog] nil)
      (assoc-in [:db :patients :selection] nil)
      (assoc :dispatch [:patients/patients-reload]))))


(defn dialog-delete []
  (let [show-dialog (rf/subscribe [:patients/show-dialog])
        selection (rf/subscribe [:patients/selection])]
    (when (and @selection (= @show-dialog :delete))
      [:> Dialog
        {:closed (not= @show-dialog :delete)
         :onClose #(rf/dispatch [:patients/show-dialog nil])
         :title "Delete patient"
         :modal true}
        [:div {:style {:padding "30px 20px"} :className "f-full"}
         [:p (str "Delete paptient: " (get-in @selection [:resource "patient_name"])
                                 " (" (timestamp->human-date (get-in @selection [:resource "birth_date"])) ")?")]]

     [:div {:className "dialog-button"}
      [:> LinkButton {:onClick #(rf/dispatch [:patients/send-event-delete])
                      :style {:float "left" :width "80px"}} "Yes"]        
      [:> LinkButton {:onClick #(rf/dispatch [:patients/show-dialog nil])
                      :style {:width "80px"}} "No"]]])))
