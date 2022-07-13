(ns backend.core
  (:gen-class)
  (:require
   [backend.context :refer [ctx]]
   [org.httpkit.server :refer [run-server send! with-channel on-close on-receive]]
   [compojure.core :refer [defroutes GET]]
   [compojure.route :refer [resources]]
   [hiccup.core :as hp]
   [clojure.edn]
   [schema.core :as schema]
   [backend.ws :as ws]))

(defonce server-stop-func (atom nil))

(defn index-page [_]
(hp/html [:html
  [:head
   [:title "Patients CRUD application"]
   [:link {:href "themes/gray/easyui.css", :type "text/css", :rel "stylesheet"}]
   [:link {:href "themes/icon.css", :type "text/css", :rel "stylesheet"}]
   [:link {:href "themes/react.css", :type "text/css", :rel "stylesheet"}]
  [:body
   [:div#app]
   [:script (str " WS_URL = '" (:ws-url ctx) "'; ")]
   [:script {:src "js/main.js"}]]]]))

(defroutes app
  (GET "/" [] index-page)
  (GET "/ws" [] (partial ws/handler ctx))
  (resources "/"))

(defn run-server-at-port [port]
  (run-server #'app {:port port}))

(defn -main []

  (when (nil? (:ws-url ctx))
    (throw (Exception. "Need env var 'WS_URL'")))

  (when (nil? (:port ctx))
    (throw (Exception. "Need env var 'PORT'")))

  (reset! server-stop-func (run-server #'app {:port (Integer/parseInt (:port ctx))}))
  (println "Run server on localhost at port: " (:port ctx)))

;(@server-stop-func)
;(-main)
