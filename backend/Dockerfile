FROM clojure:openjdk-17

WORKDIR /app

COPY deps.edn .

RUN clj -P

COPY src ./src
COPY test ./test
COPY test_runner.clj .

CMD ["clj", "-M:run"]


