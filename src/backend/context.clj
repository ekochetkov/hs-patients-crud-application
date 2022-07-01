(ns backend.context)

(def ctx {:db-spec (System/getenv "DATABASE_URL")})
