(ns volis.server
  (:require
   [compojure.core :refer [GET POST defroutes]]
   [compojure.route :as route]
   [hiccup.core :refer [html]]
   [cheshire.core :as json]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.middleware.multipart-params :refer [wrap-multipart-params]]
   [ring.middleware.params :refer [wrap-params]]
   [taoensso.timbre :as log]
   [volis.db :as db]
   [volis.csv :as csv]
   [volis.migrate :as migrate]
   [volis.env :as env])
  (:gen-class))


;; =========================
;; Helpers de Response
;; =========================

(defn response
  ([body]
   {:status 200
    :headers {"Content-Type" "text/html"}
    :body body})
  ([status body]
   {:status status
    :headers {"Content-Type" "text/html"}
    :body body}))

(defn json-response
  ([data]
   {:status 200
    :headers {"Content-Type" "application/json"}
    :body (json/generate-string data)})
  ([status data]
   {:status status
    :headers {"Content-Type" "application/json"}
    :body (json/generate-string data)}))

(defn render-table [rows]
  (html
   [:table {:border "1" :cellpadding "6" :style "border-collapse: collapse;"}
    [:thead
     [:tr
      [:th "Date"]
      [:th "Type"]
      [:th "Activity"]
      [:th "Unit"]
      [:th "Planned"]
      [:th "Executed"]]]
    [:tbody
     (for [r rows]
       [:tr
        [:td (:planned/date r)]
        [:td (:planned/type r)]
        [:td (:planned/activity r)]
        [:td (:planned/unit r)]
        [:td (:planned/planned r)]
        [:td (:planned/executed r)]])]]))

(defroutes routes
  (GET "/health" []
    (json-response {:status "OK"}))
  
  (POST "/upload/planned" {params :params}
    (let [file (get-in params ["file" :tempfile])]
      (if file
        (do
          (csv/import-planned! file)
          (json-response {:status "ok" :message "Planned CSV imported"}))
        (response 400 "File not found"))))
  
  (POST "/upload/executed" {params :params}
    (let [file (get-in params ["file" :tempfile])]
      (if file
        (do
          (csv/import-executed! file)
          (json-response {:status "ok" :message "Executed CSV imported"}))
        (response 400 "File not found"))))

  (GET "/report" {params :params}
    (let [filters
          {:date (get params "date")
           :type (get params "type")
           :activity (get params "activity")}
          rows (db/get-report filters)]
      (json-response rows)))
  (route/not-found "Not Found"))

(def app
  (-> routes
      ;; Permite chamadas do frontend (porta 8080 → 3000)
      (wrap-cors
       :access-control-allow-origin [#"http://localhost:8080"]
       :access-control-allow-methods [:get :post :options]
       :access-control-allow-headers [:content-type])
      wrap-multipart-params
      wrap-params))

(defn -main [& _]
  (log/warnf "Starting Volis Server at port %s" env/server-port)
  ;; Cria tabelas automaticamente
  (migrate/migrate!)
  (run-jetty app {:port env/server-port :join? false}))

