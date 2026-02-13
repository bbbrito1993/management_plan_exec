(ns volis.server-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [clojure.string :as str]
   [volis.server :as server]
   [ring.mock.request :as mock]))


;; =========================
;; Route Tests
;; =========================

(deftest test-healthcheck
  (testing "GET /health should return 200 OK"
    (let [response (server/app (mock/request :get "/health"))]
      (is (= (:status response) 200))
      (is (= (:body response) "OK")))))


(deftest test-not-found
  (testing "GET /nonexistent should return 404"
    (let [response (server/app (mock/request :get "/nonexistent"))]
      (is (= (:status response) 404)))))


(deftest test-cors-headers
  (testing "response should have CORS headers"
    (let [response (server/app (-> (mock/request :get "/health")
                                   (mock/header "Origin" "http://localhost:8080")))]
      (is (contains? (:headers response) "Access-Control-Allow-Origin")))))


;; =========================
;; Response Helper Tests
;; =========================

(deftest test-response
  (testing "response with single argument should return 200"
    (let [resp (server/response "Hello")]
      (is (= (:status resp) 200))
      (is (= (:body resp) "Hello"))))

  (testing "response with status and body"
    (let [resp (server/response 404 "Not Found")]
      (is (= (:status resp) 404))
      (is (= (:body resp) "Not Found"))))

  (testing "response should have Content-Type header"
    (let [resp (server/response "test")]
      (is (= (get-in resp [:headers "Content-Type"]) "text/html")))))


;; =========================
;; Render Table Tests
;; =========================

(deftest test-render-table
  (testing "render-table should create HTML table"
    (let [rows [{:planned/date "2026-02-10"
                 :planned/type "type1"
                 :planned/activity "activity1"
                 :planned/unit "unit1"
                 :planned/planned 100
                 :planned/executed 50}]
          html (server/render-table rows)]
      (is (string? html))
      (is (str/includes? html "<table"))
      (is (str/includes? html "</table>"))
      (is (str/includes? html "2026-02-10"))
      (is (str/includes? html "type1"))))

  (testing "render-table should handle empty rows"
    (let [html (server/render-table [])]
      (is (string? html))
      (is (str/includes? html "<table")))))


;; =========================
;; API Route Tests
;; =========================

(deftest test-upload-planned-without-file
  (testing "POST /upload/planned without file should return 400"
    (let [response (server/app (mock/request :post "/upload/planned"))]
      (is (= (:status response) 400))
      (is (= (:body response) "File not found")))))


(deftest test-upload-executed-without-file
  (testing "POST /upload/executed without file should return 400"
    (let [response (server/app (mock/request :post "/upload/executed"))]
      (is (= (:status response) 400))
      (is (= (:body response) "File not found")))))


(deftest test-report-without-filters
  (testing "GET /report without filters should return 200 with HTML table"
    (let [response (server/app (mock/request :get "/report"))
          body (:body response)]
      (is (= (:status response) 200))
      (is (string? body))
      (is (str/includes? body "<table"))
      (is (str/includes? body "<thead"))
      (is (str/includes? body "<tbody"))
      (is (str/includes? body "</table>"))
      ;; Validar estrutura de colunas esperadas
      (is (str/includes? body "Date"))
      (is (str/includes? body "Type"))
      (is (str/includes? body "Activity")))))


(deftest test-report-with-date-filter
  (testing "GET /report with date filter should return 200"
    (let [request (-> (mock/request :get "/report")
                      (mock/query-string {"date" "2026-02-10"}))
          response (server/app request)
          body (:body response)]
      (is (= (:status response) 200))
      (is (str/includes? body "<table"))
      ;; Verificar que a tabela tem headers
      (is (str/includes? body "Date"))
      (is (str/includes? body "Planned"))
      (is (str/includes? body "Executed")))))


(deftest test-report-with-multiple-filters
  (testing "GET /report with multiple filters returns valid HTML table"
    (let [request (-> (mock/request :get "/report")
                      (mock/query-string {"date" "2026-02-10"
                                          "type" "tipo1"
                                          "activity" "atividade1"}))
          response (server/app request)
          body (:body response)]
      (is (= (:status response) 200))
      (is (string? body))
      ;; Mostra o retorno (truncado se muito grande)
      (when (> (count body) 200)
        (println "Response body (first 200 chars):" (subs body 0 200) "..."))
      ;; Valida estrutura básica
      (is (str/includes? body "<table"))
      (is (str/includes? body "</table>")))))



(deftest test-response-headers
  (testing "All responses should have Content-Type header"
    (let [response (server/app (mock/request :get "/health"))]
      (is (= (get (:headers response) "Content-Type") "text/html")))))
