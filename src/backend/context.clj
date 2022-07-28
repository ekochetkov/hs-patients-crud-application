(ns backend.context)

(def ctx {:db-spec (System/getenv "DATABASE_URL")
          :ws-url (System/getenv "WS_URL")
          :port (System/getenv "PORT")})

(when (nil? (:db-spec ctx))
  (throw (Exception. "Need env var 'DATABASE_URL'")))

(println "-------")
(doall
(for [c (:db-spec ctx)]
  (println ">" c "<")))
(println "-------")
