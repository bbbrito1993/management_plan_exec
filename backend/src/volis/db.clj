(ns volis.db
  (:require
   [next.jdbc :as jdbc]
   [next.jdbc.sql :as sql]
   [clojure.string :as str]
   [volis.env :as env]))


(def datasource
  (jdbc/get-datasource env/db-config))

(defn insert-planned!
  "Insere lista de atividades planejadas"
  [rows]

  (jdbc/with-transaction [tx datasource]

    (doseq [row rows]
      (sql/insert! tx :planned row))))


(defn insert-executed!
  "Insere lista de atividades executadas"
  [rows]

  (jdbc/with-transaction [tx datasource]

    (doseq [row rows]
      (sql/insert! tx :executed row))))

(defn empty->nil [s]
  (when (and s (not (str/blank? s)))
    s))


(defn get-report
  "Retorna relatório com filtros opcionais"

  [{:keys [date type activity]}]

  (let [date (empty->nil date)
        type (empty->nil type)
        activity (empty->nil activity)]
    (jdbc/execute!
     datasource
     ["SELECT
         p.date,
         p.type,
         p.activity,
         p.unit,

         SUM(p.value)    AS planned,
         COALESCE(SUM(e.value),0) AS executed

       FROM planned p

       LEFT JOIN executed e
         ON p.date = e.date
        AND p.type = e.type
        AND p.activity = e.activity

       WHERE
         (?::date IS NULL OR p.date = ?::date)
         AND (?::text IS NULL OR p.type = ?::text)
         AND (?::text IS NULL OR p.activity = ?::text)

       GROUP BY
         p.date, p.type, p.activity, p.unit

       ORDER BY p.date"

      date date
      type type
      activity activity])))
