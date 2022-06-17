(ns frontend.layout
  (:require
   ["rc-easyui" :refer [DataGrid GridColumn Layout LayoutPanel Tree LinkButton Dialog Form TextBox Label DateBox]]
   [reagent.core :as r]
   [reagent.dom :as rdom]   
   [re-frame.core :as rf]
   [goog.string :as gstring]
   [websocket-fx.core :as wfx]

   [frontend.patients :as patients]
   ))

(def menu [{:text "Clinic"
            :children [{:id "about"
                        :text "About"}
                       {:id "patients"
                        :text "Patients"}]}])
    
(defn menu-tree []
  [:> Tree {:data menu  
                                        ;            :selection {:id "about" :text "About"}
                                        ;            :selection (clj->js {"id" "about" "text" "About"})
;            :selection (fn [x] true)
            :on-selection-change (fn [node]
                                   (if-let [id (.-id node)]
                                     (rf/dispatch [:menu-tree-element-click id])))}])

(rf/reg-sub :center #(:center %))

(rf/reg-event-db
 :menu-tree-element-click
 (fn [app-state [_ menu-item-id]]
   (js/console.log "click menu" menu-item-id)
   (assoc app-state :center menu-item-id)))



(defn ui []
  (let [center (rf/subscribe [:center])]
    [:> Layout  {:style {"width" "100%" "height" "100%"}}
      [:> LayoutPanel {:region "north" :style {"height" "75px"}}
       [:div {:style {:text-align "center"}}
        [:h1 "Patients CRUD application"]]]
      [:> LayoutPanel {:region "west" :style {"width" "150px"}}
        [menu-tree]]
      [:> LayoutPanel {:region "center" :style {"height" "100%"}}
        (case @center
        "patients" [patients/ui] ;[:div [dialog-create] [table]]
        "about" " about here"
        "empty ...")]]))
