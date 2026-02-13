(ns volis.csv
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [volis.db :as db]))

(defn parse-date
  "Converte YYYY-MM-DD para java.sql.Date.
   Retorna nil se inválido."
  [s]
  (try
    (java.sql.Date/valueOf s)
    (catch Exception _
      (println "Invalid date:" s)
      nil)))


(defn parse-double-str
  "Converte string para double.
   Retorna 0.0 se inválido."
  [s]
  (try
    (Double/parseDouble s)
    (catch Exception _
      (println "Invalid number:" s)
      0.0)))

(defn normalize
  "Remove espaços extras"
  [s]
  (when s
    (str/trim s)))

(defn parse-file
  "Lê CSV e transforma em lista de mapas"
  [file]
  (with-open [reader (io/reader file)]
    (let [[header & rows] (csv/read-csv reader)]
      (doall
       (->> rows
            (map-indexed
             (fn [idx [date type activity unit value]]
               (let [d (parse-date date)]
                 (when-not d
                   (println "Skipping line" (+ idx 2)))
                 {:date d
                  :type (normalize type)
                  :activity (normalize activity)
                  :unit (normalize unit)
                  :value (parse-double-str value)})))
            ;; Remove linhas com data inválida
            (filter :date))))))


;; =========================
;; Import
;; =========================

(defn import-planned!
  "Importa CSV planned no banco"

  [file]

  (println "Importing planned CSV...")

  (let [data (parse-file file)]

    (println "Valid rows:" (count data))

    (db/insert-planned! data)

    (println "Planned import finished")))


(defn import-executed!
  "Importa CSV executed no banco"

  [file]

  (println "Importing executed CSV...")

  (let [data (parse-file file)]

    (println "Valid rows:" (count data))

    (db/insert-executed! data)

    (println "Executed import finished")))
