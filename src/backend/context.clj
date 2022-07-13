(ns backend.context)

(def ctx {:db-spec (System/getenv "DATABASE_URL")
          :ws-url (System/getenv "WS_URL")
          :port (System/getenv "PORT")})

;; TODO: Check envs dependent on jobs
;;       need other mechanism check on events
;;(when (nil? (:db-spec ctx))
;;  (throw (Exception. "Need env var 'DATABASE_URL'")))
