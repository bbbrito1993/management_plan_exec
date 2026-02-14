(ns volis.csv
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [taoensso.timbre :as log]
   [volis.db :as db]))

(defn parse-date
  "Converte YYYY-MM-DD para java.sql.Date.
   Retorna nil se inválido."
  [s]
  (try
    (java.sql.Date/valueOf s)
    (catch Exception _
      (log/warnf "Invalid date: %s" s)
      nil)))

(defn parse-double-str
  "Converte string para double.
   Retorna 0.0 se inválido."
  [s]
  (try
    (Double/parseDouble s)
    (catch Exception _
      (log/warnf "Invalid number: %s" s)
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
    (let [[_header & rows] (csv/read-csv reader)]
      (doall
       (->> rows
            (map-indexed
             (fn [idx [date type activity unit value]]
               (let [d (parse-date date)]
                 (when-not d
                   (log/warnf "Skipping line %d" (+ idx 2)))
                 {:date d
                  :type (normalize type)
                  :activity (normalize activity)
                  :unit (normalize unit)
                  :value (parse-double-str value)})))
            ;; Remove linhas com data inválida
            (filter :date))))))

(defn import-planned!
  "Importa CSV planned no banco"
  [file]
  (log/infof "Importing planned CSV: %s" file)
  (let [data (parse-file file)]
    (log/infof "Valid rows: %d" (count data))
    (db/insert-planned! data)
    (log/info "Planned import finished")))

(defn import-executed!
  "Importa CSV executed no banco"
  [file]
  (log/infof "Importing executed CSV: %s" file)
  (let [data (parse-file file)]
    (log/infof "Valid rows: %d" (count data))
    (db/insert-executed! data)
    (log/info "Executed import finished")))
