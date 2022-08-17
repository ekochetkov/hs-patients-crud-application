(ns backend.pages
  (:require
   [backend.context :refer [ctx]]   
   [hiccup.core :as hp]
   [clojure.java.jdbc :as jdbc]))

(defn style [& info]
  (.trim (apply str (map #(let [[k v] %]
                               (str (name k) ":" v "; "))
                               (apply hash-map info)))))
(defn k8s-pod-info []
  [:div {:style (style :position "fixed"
                       :left 0
                       :top 0
                       :z-index 1000
                       :background "#6bb5f77a"
                       :padding "1px")}
    [:p {:style (style :margin 0
                       :font-size "small")}
     (str "Run in Kubernetes cluster."
          " nodeName: "  (System/getenv "K8S_POD_NODE_NAME")
          "; image: "     (System/getenv "K8S_POD_IMAGE")
          "; podIP: "     (System/getenv "K8S_POD_IP")
          "; podName: "   (System/getenv "K8S_POD_NAMESPACE") "/" (System/getenv "K8S_POD_NAME")
          "; deployDatetime: " (System/getenv "K8S_POD_DEPLOY_DATETIME"))]])

(defn index-page [_]
  (hp/html [:html
    [:head
     [:title "Patients CRUD application"]
     [:link {:href "themes/gray/easyui.css", :type "text/css", :rel "stylesheet"}]
     [:link {:href "themes/icon.css", :type "text/css", :rel "stylesheet"}]
     [:link {:href "themes/react.css", :type "text/css", :rel "stylesheet"}]]
    [:body
     (when (System/getenv "RUN_IN_K8S")
       (k8s-pod-info))
     [:div#app]
     [:script (str " WS_URL = '" (:ws-url ctx) "'; ")]
     [:script {:src "js/main.js"}]]]))

(defn health [ctx _]
  (try
    (jdbc/query (:db-spec ctx) "select version()")
    "ok"
    (catch Exception e
      (println e)
      "fail")))
