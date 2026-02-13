
# Volis - Aplicação Clojure com Docker, PostgreSQL e Frontend

Volis é uma aplicação de entrada técnica desenvolvida com backend em Clojure, banco de dados PostgreSQL e frontend em HTML/JS servido por Nginx. Permite o upload de arquivos CSV para atividades planejadas e executadas, geração de relatórios filtráveis e visualização de dados em um dashboard simples. Suporta execução via Docker Compose (para produção/teste) ou Docker Run (para desenvolvimento com logs em tempo real).

## Produção/Teste (com Docker Compose)

```bash
docker-compose up -d
```

Acesse:
- Frontend: http://localhost:8080
- Backend API: http://localhost:3000
- PostgreSQL: localhost:5432

**Credenciais PostgreSQL:**
- User: `volis`
- Password: `volis`
- Database: `volis`
- Host: `localhost` (acesso local) ou `db` (acesso via docker)
- Porta: `5432`


## Subir serviços individualmente

```bash
# Banco de dados
docker-compose up -d db

# 2. Esperar alguns segundos e subir o backend
docker-compose up -d backend

# Frontend
docker-compose up -d frontend

# Ver logs
docker-compose logs -f [db|backend|frontend]

# Derrubar serviço específico
docker-compose stop [db|backend|frontend]
```

## Rodar testes

```bash
# Abrir bash no container do backend
docker run --rm -it -v ./backend:/app volis_backend /bin/bash

# Dentro do container, executar os testes
clj -M:test
```

## Desenvolvimento (com Docker Run)

Para desenvolvimento, use `docker run` para subir os serviços individualmente e ver os logs em tempo real. Isso facilita a depuração e adição de prints no código.

Primeiro, crie uma rede Docker para comunicação entre os containers:

```bash
docker network create volis_net
```

### Banco de dados (PostgreSQL)

```bash
docker run --name volis_db --network volis_net -e POSTGRES_DB=volis -e POSTGRES_USER=volis -e POSTGRES_PASSWORD=volis -p 5432:5432 -v pgdata:/var/lib/postgresql/data postgres:15
```

**Exemplo de output (logs):**
```
2026-02-12 10:00:00.000 UTC [1] LOG:  database system is ready to accept connections
```

### Backend (Clojure)

Primeiro, construa a imagem:

```bash
docker build -t volis_backend ./backend
```

Em seguida, execute o container:

```bash
docker run --name volis_backend --network volis_net -p 3000:3000 -e DB_HOST=volis_db -e DB_NAME=volis -e DB_USER=volis -e DB_PASS=volis -v ./backend:/app volis_backend
```

**Exemplo de output (logs):**
```
INFO  volis.server - Starting server on port 3000
INFO  volis.db - Connected to database
```

#### Rodar testes no container (desenvolvimento)

Para rodar os testes dentro do container Docker, use:

```bash
docker run --rm -v ./backend:/app volis_backend clj -M:test
```

**Exemplo de output:**
```
Running tests...
Testing volis.csv-test
Ran 5 tests containing 10 assertions.
0 failures, 0 errors.
Testing volis.server-test
Ran 3 tests containing 6 assertions.
0 failures, 0 errors.
```

### Frontend (Nginx)

```bash
docker run --name volis_frontend -p 8080:80 -v ./frontend:/usr/share/nginx/html:ro nginx:alpine
```

**Exemplo de output (logs):**
```
nginx: [notice] started process 1
```

Para parar e remover os containers de desenvolvimento:

```bash
docker stop volis_db volis_backend volis_frontend
docker rm volis_db volis_backend volis_frontend
docker network rm volis_net
```

## Derrubar tudo

```bash
docker-compose down
```

## Testes funcionais (exemplos com `curl`)

Use estes exemplos para verificar manualmente cada endpoint da API. Os comandos assumem que os serviços estão levantados (`docker-compose up -d`).

- Healthcheck

```bash
curl -i http://localhost:3000/health
# Esperado: 200 OK com body 'OK'
```

- Relatório (sem filtros)

```bash
curl -i "http://localhost:3000/report"
# Esperado: 200 OK e um HTML contendo uma tabela (<table>, <thead>, <tbody>)
```

- Relatório (com filtros)

```bash
curl -i "http://localhost:3000/report?date=2026-02-10&type=tipo1&activity=atividade1"
# Esperado: 200 OK e tabela filtrada (ou tabela vazia se não houver dados)
```

- Upload de CSV (planned)

```bash
curl -i -X POST -F "file=@data/benedito-2026-02-05_planned.csv" http://localhost:3000/upload/planned
# Esperado: 200 OK com mensagem 'Planned CSV imported' e registros inseridos no DB
```

- Upload de CSV (executed)

```bash
curl -i -X POST -F "file=@data/benedito-2026-02-05_executed.csv" http://localhost:3000/upload/executed
# Esperado: 200 OK com mensagem 'Executed CSV imported' e registros inseridos no DB
```

- Verificar dados no PostgreSQL (dentro do container)

```bash
# Abrir psql no container
docker-compose exec db psql -U volis -d volis

-- Dentro do psql, listar tabelas e dados
\dt
SELECT * FROM planned LIMIT 10;
SELECT * FROM executed LIMIT 10;
\q
```


