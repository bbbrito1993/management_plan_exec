(ns volis.csv-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [volis.csv :as csv]
   [clojure.string :as cstr])
  (:import (java.io File)))


(defn create-temp-csv
  "Cria um arquivo CSV temporário com os dados fornecidos"
  [rows]
  (let [file (File/createTempFile "test" ".csv")
        content (cstr/join "\n"
                  (map #(cstr/join "," %)
                    (cons ["date" "type" "activity" "unit" "value"] rows)))]
    (spit file content)
    file))


(deftest test-parse-date
  (testing "should parse valid date"
    (let [result (csv/parse-date "2026-02-10")]
      (is (not (nil? result)))
      (is (instance? java.sql.Date result))))

  (testing "should return nil for invalid date"
    (let [result (csv/parse-date "invalid-date")]
      (is (nil? result))))

  (testing "should handle empty string"
    (let [result (csv/parse-date "")]
      (is (nil? result)))))


(deftest test-parse-double
  (testing "should parse valid number"
    (let [result (csv/parse-double-str "123.45")]
      (is (= result 123.45))))

  (testing "should parse integer"
    (let [result (csv/parse-double-str "100")]
      (is (= result 100.0))))

  (testing "should return 0.0 for invalid number"
    (let [result (csv/parse-double-str "invalid")]
      (is (= result 0.0))))

  (testing "should handle empty string"
    (let [result (csv/parse-double-str "")]
      (is (= result 0.0)))))


(deftest test-normalize
  (testing "should trim whitespace"
    (let [result (csv/normalize "  activity  ")]
      (is (= result "activity"))))

  (testing "should handle nil"
    (let [result (csv/normalize nil)]
      (is (nil? result))))

  (testing "should keep normal string"
    (let [result (csv/normalize "normal")]
      (is (= result "normal")))))

(deftest test-parse-file
  (testing "should parse valid CSV file"
    (let [csv-file (create-temp-csv
                     [["2026-02-10" "type1" "activity1" "unit1" "100"]
                      ["2026-02-11" "type2" "activity2" "unit2" "200"]])
          result (csv/parse-file csv-file)]
      (is (= (count result) 2))
      (is (= (:type (first result)) "type1"))
      (is (= (:value (first result)) 100.0))))

  (testing "should skip invalid dates"
    (let [csv-file (create-temp-csv
                    [["2026-02-10" "type1" "activity1" "unit1" "100"]
                     ["invalid" "type2" "activity2" "unit2" "200"]])
          result (csv/parse-file csv-file)]
      (is (= [{:date #inst "2026-02-10T00:00:00.000-00:00"
               :type "type1"
               :activity "activity1"
               :unit "unit1"
               :value 100.0}] result))))

  (testing "should normalize strings"
    (let [csv-file (create-temp-csv
                    [["2026-02-10" "  type  " "  activity  " "  unit  " "100"]])
          result (csv/parse-file csv-file)]
      (is (= result [{:date #inst "2026-02-10" :type "type" :activity "activity" :unit "unit" :value 100.0}]))))

  (testing "should handle invalid numbers"
    (let [csv-file (create-temp-csv
                     [["2026-02-10" "type1" "activity1" "unit1" "invalid"]])
          result (csv/parse-file csv-file)
          first-row (first result)]
      (is (= (:value first-row) 0.0)))))
