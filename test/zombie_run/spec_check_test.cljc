(ns zombie-run.spec-check-test
  (:require [clojure.spec.test.alpha :as stest]
            [zombie-run.game]

    #?(:cljs [cljs.test :refer-macros [is are deftest testing]]
       :clj
            [clojure.test :refer :all])))

(deftest check-specs-test
  ; using clojure reader conditionals here shouldn't
  ; be necessary when the clj part does contain the latest changes
  (let [report (stest/check #?(:clj (stest/enumerate-namespace 'zombie-run.game)
                               :cljs `zombie-run.game))]
    (if (zero? (count report))
      (throw (ex-info "No spec reports found." {}))
      (doseq [report report]
        (let [result (get-in report [:clojure.spec.test.check/ret :result])]
          (when-not (true? result)
            (prn (:failure report)))
          (is (true? result)))))))
