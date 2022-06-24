(ns backend.ws-routes
  (:require [schema.core :as s]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc])
  (:import [java.sql Timestamp]
           [java.time Instant]))

(def db (System/getenv "DATABASE_URL"))

(defn patients-data [where limit offset]
  (jdbc/query db "select uuid, resource from patients where deleted is null" {:keywordize? false}))

(extend-protocol jdbc/IResultSetReadColumn
  org.postgresql.util.PGobject
  (result-set-read-column [pg-obj _rsmeta _idx]

    (let [pg-val (.getValue pg-obj)
          pg-type (.getType pg-obj)]

      (case pg-type
        ("json" "jsonb")
        (json/read-str pg-val)
        pg-obj))))

(extend-protocol jdbc/ISQLValue
  clojure.lang.IPersistentCollection
  (sql-value [val]
    (doto (new org.postgresql.util.PGobject)
      (.setType "jsonb")
      (.setValue (json/write-str val)))))

(defn patients-create-fn [resource]
  (let [uuid (java.util.UUID/randomUUID)]
    (jdbc/insert! db :patients {:uuid uuid
                                :deleted nil        
                                :resource resource})
  uuid))

(def ws-request-routes
  {:patients/data patients-data
   :patients/create patients-create-fn
   :patients/delete
     (fn [uuid]
       (jdbc/update! db :patients
          {:deleted (Timestamp/from (Instant/now))} ["uuid = ?::uuid" uuid]))})
