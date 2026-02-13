(ns volis.env)

;; =========================
;; Funções auxiliares
;; =========================

(defn get-env
  "Obtém variável de ambiente com valor padrão"
  [key default]
  (or (System/getenv key) default))

;; =========================
;; Variáveis de Banco de Dados
;; =========================

(def db-host (get-env "DB_HOST" "localhost"))
(def db-port (Integer/parseInt (get-env "DB_PORT" "5432")))
(def db-name (get-env "DB_NAME" "volis"))
(def db-user (get-env "DB_USER" "volis"))
(def db-pass (get-env "DB_PASS" "volis"))

;; =========================
;; Variáveis de Servidor
;; =========================

(def server-port (Integer/parseInt (get-env "SERVER_PORT" "3000")))

;; =========================
;; Configuração do Banco
;; =========================

(def db-config
  {:dbtype "postgresql"
   :host db-host
   :port db-port
   :dbname db-name
   :user db-user
   :password db-pass})
