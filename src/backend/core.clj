(ns backend.core
  (:gen-class)
  (:require
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
   [:script (str " WS_URL = '" (System/getenv "WS_URL") "'; ")]
   [:script {:src "js/main.js"}]]]]))

(def ctx {:db-spec (System/getenv "DATABASE_URL")})

(defroutes app
  (GET "/" [] index-page)
  (GET "/ws" [] (partial ws/handler ctx))
  (resources "/"))

(defn run-server-at-port [port]
  (run-server #'app {:port port}))

(defn -main []
  (println "Entry main")
  (reset! server-stop-func (run-server #'app {:port (Integer/parseInt (System/getenv "PORT"))}))
  (println "Run server on localhost at port: 9009"))

;(@server-stop-func)
;(-main)
