(ns volis.server-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [ring.mock.request :as mock]
   [volis.db :as db]
   [volis.csv :as csv]
   [volis.server :as server]))

(defn mock-db-fixture [f]
  (with-redefs [db/get-report (fn [_] [{:planned/date "2026-02-10"
                                        :planned/type "type1"
                                        :planned/activity "activity1"
                                        :planned/unit "unit1"
                                        :planned/planned 100
                                        :planned/executed 50}])]
    (f)))

(use-fixtures :each mock-db-fixture)

(deftest test-healthcheck
  (testing "GET /health should return 200 OK with JSON"
    (let [response (server/app (mock/request :get "/health"))]
      (is {:status 200
           :headers {"Content-Type" "application/json"}
           :body "{\"status\":\"OK\"}"} response))))


(deftest test-not-found
  (testing "GET /nonexistent should return 404"
    (let [response (server/app (mock/request :get "/nonexistent"))]
      (is (= {:status 404
              :headers {"Content-Type" "text/html; charset=utf-8"},
              :body "Not Found"} response)))))

(deftest test-cors-headers
  (testing "response should have CORS headers"
    (let [response (server/app (-> (mock/request :get "/health")
                                   (mock/header "Origin" "http://localhost:8080")))]
      (is (contains? (:headers response) "Access-Control-Allow-Origin")))))

(deftest test-response
  (testing "response with single argument should return 200"
    (let [resp (server/response "Hello")]
      (is (= {:status 200 :headers {"Content-Type" "text/html"}, :body "Hello"} resp))))

  (testing "response with status and body"
    (let [resp (server/response 404 "Not Found")]
      (is (= {:status 404 :headers {"Content-Type" "text/html"} :body "Not Found"} resp))))

  (testing "response should have Content-Type header"
    (let [resp (server/response "test")]
      (is (= (get-in resp [:headers "Content-Type"]) "text/html")))))

(deftest test-upload-planned-without-file
  (testing "POST /upload/planned without file should return 400 with JSON error"
    (let [response (server/app (mock/request :post "/upload/planned"))]
      (is (= {:status 400
              :headers {"Content-Type" "application/json"}
              :body "{\"error\":\"File not found\"}"} response)))))

(deftest test-upload-executed-without-file
  (testing "POST /upload/executed without file should return 400 with JSON error"
    (let [response (server/app (mock/request :post "/upload/executed"))]
      (is (= (:status response) 400))
      (is (= (get (:headers response) "Content-Type") "application/json"))
      (is (str/includes? (:body response) "\"error\":\"File not found\"")))))


(deftest test-upload-planned-with-file
  (testing "POST /upload/planned with file should return 200 with JSON success"
    (with-redefs [csv/import-planned! (fn [_] nil)]
      (let [response (server/app (assoc (mock/request :post "/upload/planned") :params {"file" {:tempfile (java.io.File/createTempFile "test" ".csv")}}))]
        (is (= {:status 200 :headers {"Content-Type" "application/json"} :body "{\"status\":\"ok\",\"message\":\"Planned CSV imported\"}"} response))))))

(deftest test-upload-executed-with-file
  (testing "POST /upload/executed with file should return 200 with JSON success"
    (with-redefs [csv/import-executed! (fn [_] nil)]
      (let [response (server/app (assoc (mock/request :post "/upload/executed") :params {"file" {:tempfile (java.io.File/createTempFile "test" ".csv")}}))]
        (is (= {:status 200 :headers {"Content-Type" "application/json"} :body "{\"status\":\"ok\",\"message\":\"Executed CSV imported\"}"} response))
        (is (= (get (:headers response) "Content-Type") "application/json"))
        (is (str/includes? (:body response) "\"status\":\"ok\""))))))


(deftest test-report-without-filters
  (testing "GET /report without filters should return 200 with JSON array"
    (let [response (server/app (mock/request :get "/report"))
          body (:body response)]
      (is (= (:status response) 200))
      (is (= (get (:headers response) "Content-Type") "application/json"))
      (is (string? body))
      ;; Verificar se é um array JSON (começa com [)
      (is (str/starts-with? body "["))
      (is (str/ends-with? body "]")))))

(deftest test-report-with-date-filter
  (testing "GET /report with date filter should return 200 with JSON"
    (let [request (-> (mock/request :get "/report")
                      (mock/query-string {"date" "2026-02-10"}))
          response (server/app request)
          body (:body response)]
      (is (= (:status response) 200))
      (is (= (get (:headers response) "Content-Type") "application/json"))
      (is (str/starts-with? body "["))
      (is (str/ends-with? body "]")))))

(deftest test-report-with-multiple-filters
  (testing "GET /report with multiple filters returns valid JSON array"
    (let [request (-> (mock/request :get "/report")
                      (mock/query-string {"date" "2026-02-10"
                                          "type" "tipo1"
                                          "activity" "atividade1"}))
          response (server/app request)]
      (is (= {:status 200,
              :headers {"Content-Type" "application/json"}
              :body "[{\"planned/date\":\"2026-02-10\",\"planned/type\":\"type1\",\"planned/activity\":\"activity1\",\"planned/unit\":\"unit1\",\"planned/planned\":100,\"planned/executed\":50}]"} response)))))

(deftest test-response-headers
  (testing "All responses should have Content-Type header"
    (let [response (server/app (mock/request :get "/health"))]
      (is (= (get (:headers response) "Content-Type") "application/json")))))
