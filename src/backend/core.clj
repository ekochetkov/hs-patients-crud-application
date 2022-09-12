(ns backend.core
  (:gen-class)
  (:require
   [backend.context :refer [ctx]]
   [backend.pages :refer [index-page health]]
   [org.httpkit.server :refer [run-server]]
   [compojure.core :refer [defroutes GET]]
   [compojure.route :refer [resources]]
   [backend.ws :as ws]
   [backend.patients-events]))

(defonce server-stop-func (atom nil))

(defroutes app
  (GET "/" [] index-page)
  (GET "/ws" [] (partial ws/handler ctx))
  (GET "/health" [] (partial health ctx))
  (resources "/"))

(defn run-server-at-port [port]
  (run-server #'app {:port port}))

(defn -main [& _]

  (when (nil? (:ws-url ctx))
    (throw (Exception. "Need env var 'WS_URL'")))

  (when (nil? (:port ctx))
    (throw (Exception. "Need env var 'PORT'")))

  (when (nil? (:db-spec ctx))
    (throw (Exception. "Need env var 'DATABASE_URL'")))
  
  (reset! server-stop-func (run-server #'app {:port (Integer/parseInt (:port ctx))}))
  (println "Run server on localhost at port: " (:port ctx)))

;(@server-stop-func)
;(-main)
