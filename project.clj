(defproject hs-patients-crud "0.0.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [javax.xml.bind/jaxb-api "2.3.1"]
                 [prismatic/schema "1.2.1"]
                 [org.clojure/tools.logging "1.2.4"]
                 [http-kit "2.1.18"]
                 [compojure "1.3.4"]
                 [hiccup "1.0.5"]
                 [migratus/migratus "1.3.5"]
		 [org.clojure/java.jdbc "0.6.1"]
		 [org.postgresql/postgresql "9.4-1201-jdbc41"]
                 [org.clojure/data.json "2.4.0"]
                 [stylefruits/gniazdo "1.2.1"]]

  :plugins [ [migratus-lein "0.7.3"] ]

  :main backend.core
  :aot :all)
