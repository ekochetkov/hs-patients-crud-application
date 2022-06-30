(ns backend.db
  (:require
   [clojure.data.json :as json]
   [clojure.java.jdbc :as jdbc]))

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
