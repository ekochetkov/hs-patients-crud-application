(ns backend.context)

(def ctx {:db-spec (System/getenv "DATABASE_URL")
          :ws-url (System/getenv "WS_URL")
          :port (System/getenv "PORT")})
