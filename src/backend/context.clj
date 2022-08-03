(ns backend.context)

(def ctx {:db-spec (str
                    (System/getenv "DATABASE_URL")
                    (System/getenv "DATABASE_URL_PARAMS"))
          :ws-url (System/getenv "WS_URL")
          :port (System/getenv "PORT")})
