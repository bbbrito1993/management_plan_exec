(ns volis.migrate
  (:require
   [next.jdbc :as jdbc]
   [volis.db :as db]))


(defn migrate! []

  (jdbc/execute!
   db/datasource
   ["CREATE TABLE IF NOT EXISTS planned (
      id SERIAL PRIMARY KEY,
      date DATE,
      type TEXT,
      activity TEXT,
      unit TEXT,
      value NUMERIC
    )"])

  (jdbc/execute!
   db/datasource
   ["CREATE TABLE IF NOT EXISTS executed (
      id SERIAL PRIMARY KEY,
      date DATE,
      type TEXT,
      activity TEXT,
      unit TEXT,
      value NUMERIC
    )"]))
