(ns test-runner
  (:require
   [clojure.test :refer [run-tests]]
   [volis.server-test]
   [volis.csv-test]))

(defn -main [& _]
  (let [results (run-tests 'volis.server-test 'volis.csv-test)
        failures (if (and results (:failures results)) (:failures results) 0)]
    (System/exit (if (zero? failures) 0 1))))
