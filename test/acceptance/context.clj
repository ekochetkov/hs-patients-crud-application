(ns acceptance.context
  "Module for acceptance tests"
  (:require [clojure.test :refer [join-fixtures]]
            [backend.patients-events-test :refer [fx-clear-db]]

            [etaoin.api :as e]))

(def ^:dynamic *driver*)

(declare driver)

(defn fx-print-dot-every-test [f]
  (println ".")
  (f))

(defn fx-navigate-browser [f]
  (let [app-url (System/getenv "APP_URL")]
    (when (nil? app-url)
      (throw (Exception. "Need env var 'APP_URL'")))
    (e/go *driver* app-url)
    (f)))

(defn fx-browser [f]
  (let [local-browser (System/getenv "LOCAL_BROWSER")
        selenium-grid-hub-url (System/getenv "SELENIUM_GRID_HUB_URL")
        selenium-grid-browser (System/getenv "SELENIUM_GRID_BROWSER")]
    (cond
      ;; Local testing
      (not (nil? local-browser))
         (case local-browser
            "firefox" (e/with-firefox driver (binding [*driver* driver] (f)))
            "chrome"  (e/with-chrome  driver (binding [*driver* driver] (f)))
            (throw (Exception. "LOCAL_BROWSER may be 'firefox' or 'chrome'")))
      ;; CI testing
      (not (and (nil? selenium-grid-hub-url)
                (nil? selenium-grid-browser)))
         (let [driver-opts {:webdriver-url selenium-grid-hub-url
                               :raw-capabilities {
                               :capabilities {:firstMatch [{}]
                                              :alwaysMatch {:browserName selenium-grid-browser}}
                                                  :desiredCapabilities {:browserName selenium-grid-browser}}}]
;;           (println driver-opts)
              (e/with-selenium-grid driver-opts driver (binding [*driver* driver] (f))))
      ;; Unknow testing
      :else (throw (Exception. "Unknow testing")))))
              
(def fx-default (join-fixtures [fx-print-dot-every-test
                                fx-clear-db
                                fx-browser
                                fx-navigate-browser]))
